
package komplex.tools.maven

import com.jcabi.aether.Aether
import komplex.data.openFileSetI
import komplex.dsl.tools
import komplex.model.ArtifactData
import komplex.model.ArtifactDesc
import komplex.model.BuildContext
import komplex.model.BuildResult
import komplex.utils.BuildDiagnostic
import org.slf4j.LoggerFactory
import org.sonatype.aether.repository.RemoteRepository
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.util.artifact.JavaScopes
import java.io.File
import java.nio.file.Paths
import java.util.Arrays

val tools.maven: MavenResolverRule
    get() = MavenResolverRule(komplex.model.LazyTool<MavenResolverRule, MavenResolver>("maven", { MavenResolver()} ))

val log = LoggerFactory.getLogger("komplex.tools.maven")

// separate class for separate class loading
// \todo check if moving to separate file or jar is needed for really lazy tool loading, or may be that side-by-side class will work as well
class MavenResolverRule(mavenResolver: komplex.model.Tool<MavenResolverRule>) : komplex.dsl.BasicToolRule<MavenResolverRule, komplex.model.Tool<MavenResolverRule>>(mavenResolver) {

    internal var repositoryName: String = "maven-central"
    internal var repositoryUrl: String = "http://repo1.maven.org/maven2/"
    var dir: String = "./lib"

    fun repository(name: String, url: String): MavenResolverRule {
        repositoryName = name
        repositoryUrl = url
        return this
    }

    override val targets: Iterable<ArtifactDesc> get() =
            sources.map { MavenResolvedLibraryArtifact(it as MavenLibraryArtifact) }

    fun invoke(body: MavenResolverRule.() -> Unit): MavenResolverRule {
        body()
        return this
    }
}

// resolves all sources into (FileSet) destinations, ignores targets, sources are used as a key to destinations
open class MavenResolver() : komplex.model.Tool<MavenResolverRule> {
    override val name: String = "maven"

    override fun execute(context: BuildContext, cfg: MavenResolverRule, src: Iterable<Pair<ArtifactDesc, ArtifactData?>>, tgt: Iterable<ArtifactDesc>): BuildResult {
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
            log.info("resolving [$id]")
            val deps = Aether(remoteRepos, localRepo).resolve(
                    DefaultArtifact("${id.groupId}:${id.artifactId}:${id.version}"),
                    JavaScopes.RUNTIME)
            if (deps == null) {
                log.error("resolving [$id] failed")
                return BuildResult(BuildDiagnostic.Fail)
            }
            log.info("resolved successfuly:")
            result.add(Pair(MavenResolvedLibraryArtifact(sourceDesc), openFileSetI(deps.map { Paths.get(it.file.path) })))
        }
        return BuildResult( BuildDiagnostic.Success, result)
    }
}
