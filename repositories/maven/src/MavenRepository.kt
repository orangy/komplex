
package komplex.maven

import komplex.dependencies.*
import komplex.LibraryReference
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Arrays
import java.io.File
import com.jcabi.aether.Aether
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.repository.RemoteRepository
import org.sonatype.aether.util.artifact.JavaScopes
import org.sonatype.aether.artifact.Artifact
import org.apache.maven.project.MavenProject

public open class MavenRepository(repoName: String, url: String, libPath: Path) : Repository {
    override val name: String = "$repoName (maven: $url)"
    val remoteRepos: MutableList<RemoteRepository> = Arrays.asList(RemoteRepository(repoName, "default", url))
    val localRepo: File = File(libPath.toString())

    override fun resolve(reference: LibraryReference): List<Path> {
        val names = reference.name.split("[/:]")
        val groupId = names[0]
        val artifactId = if (names.size > 1) names[1] else groupId
        val version = reference.version ?: if (names.size > 2) names[2] else null
        if (version == null)
            // assuming non-maven artifact
            return listOf()
        val deps = Aether(remoteRepos, localRepo).resolve(
          DefaultArtifact("$groupId:$artifactId:$version"),
          JavaScopes.RUNTIME)
        return deps?.map { Paths.get(it.getFile().path) }?.toArrayList() ?: listOf()
    }
}

public fun mavenRepository(repoName: String, url: String, relLibPath : String) : Repository {
    val res = MavenRepository(repoName, url , Paths.get(".", relLibPath)!!.toAbsolutePath())
    repositories.list.add(res)
    return res;
}

public fun mavenCentralRepository(relLibPath : String) : Repository =
        mavenRepository("maven-central", "http://repo1.maven.org/maven2/", relLibPath)
