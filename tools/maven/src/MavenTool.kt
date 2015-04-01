
package komplex.maven

import java.nio.file.Path
import java.nio.file.Paths
import java.util.Arrays
import java.io.File
import com.jcabi.aether.Aether
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.repository.RemoteRepository
import org.sonatype.aether.util.artifact.JavaScopes
import org.apache.maven.project.MavenProject
import komplex.tools
import kotlin.properties.Delegates
import komplex.dsl.tools
import komplex.model.BuildContext
import komplex.model.ArtifactDesc
import komplex.model.ArtifactData
import komplex.model.BuildResult
import komplex.model.Scenarios
import komplex.utils.BuildDiagnostic
import komplex.data.openFileSetI

public val tools.maven: MavenResolverRule
    get() = MavenResolverRule(komplex.model.LazyTool<MavenResolverRule, MavenResolver>("maven", { MavenResolver()} ))


// separate class for separate class loading
// \todo check if moving to separate file or jar is needed for really lazy tool loading, or may be that side-by-side class will work as well
public class MavenResolverRule(mavenResolver: komplex.model.Tool<MavenResolverRule>) : komplex.dsl.BasicToolRule<MavenResolverRule, komplex.model.Tool<MavenResolverRule>>(mavenResolver) {

    internal var repositoryName: String = "maven-central"
    internal var repositoryUrl: String = "http://repo1.maven.org/maven2/"
    public var dir: String = "./lib"

    public fun repository(name: String, url: String): MavenResolverRule {
        repositoryName = name
        repositoryUrl = url
        return this
    }

    override val targets: Iterable<ArtifactDesc> get() =
            sources.map { MavenResolvedLibraryArtifact(it as MavenLibraryArtifact) }

    public fun invoke(body: MavenResolverRule.() -> Unit): MavenResolverRule {
        body()
        return this
    }
}

// resolves all sources into (FileSet) destinations, ignores targets, sources are used as a key to destinations
public open class MavenResolver() : komplex.model.Tool<MavenResolverRule> {
    override val name: String = "maven"

    override fun execute(context: BuildContext, cfg: MavenResolverRule, src: Iterable<Pair<ArtifactDesc, ArtifactData?>>, tgt: Iterable<ArtifactDesc>): BuildResult {
//    internal fun resolveMaven(context: BuildStepContext, sources: Iterable<komplex.Artifact>, rule: MavenResolverRule): BuildResult {
        val result = arrayListOf<Pair<ArtifactDesc, ArtifactData?>>()
        val remoteRepos: MutableList<RemoteRepository> = Arrays.asList(RemoteRepository(cfg.repositoryName, "default", cfg.repositoryUrl))
        val localRepo: File = File(cfg.dir)
        // \todo error handling
        for (sourcePair in src) {
            // \todo switch to regular logging
            val sourceDesc = sourcePair.first
            if (sourceDesc !is MavenLibraryArtifact)
                return BuildResult(BuildDiagnostic.Fail)
            val id = sourceDesc.id
            println("[INFO] resolving [$id]")
            val deps = Aether(remoteRepos, localRepo).resolve(
                    DefaultArtifact("${id.groupId}:${id.artifactId}:${id.version}"),
                    JavaScopes.RUNTIME)
            if (deps == null) {
                println("[ERROR] resolving [$id] failed")
                return BuildResult(BuildDiagnostic.Fail)
            }
            println("[INFO] resolved successfuly:")
            result.add(Pair(MavenResolvedLibraryArtifact(sourceDesc), openFileSetI(deps.map { Paths.get(it.getFile().path) })))
        }
        return BuildResult( BuildDiagnostic.Success, result)
    }
}
