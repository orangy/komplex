
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
import komplex.ResolveTool
import komplex.tools
import kotlin.properties.Delegates
import komplex.BuildStepContext
import komplex.BuildResult
import komplex.Scenario
import komplex.LibraryReferenceArtifact
import komplex.LibraryWithDependenciesArtifact
import komplex.print

public val tools.maven: MavenResolverRule
    get() = MavenResolverRule()


// separate class for separate class loading
// \todo check if moving to separate file or jar is needed for really lazy tool loading, or may be that nested class will work as well
public class MavenResolverRule(override val local: Boolean = false) : komplex.Resolver.BaseRule(local) {

    override val tool by Delegates.lazy { MavenTool() }

    override fun execute(context: BuildStepContext): BuildResult
            = tool.resolveMaven(context, selectSources.get(context.scenario), this)
    override fun dryResolve(scenario: Scenario, source: komplex.Artifact): komplex.Artifact {
        if (source is LibraryReferenceArtifact) {
            val id = source.mavenId()
            return LibraryWithDependenciesArtifact("${id.groupId}:${id.artifactId}:${id.version}")
        }
        // \todo design proper error reporting
        else throw Exception("unknown reference artifact type")
    }

    internal var repositoryName: String = "maven-central"
    internal var repositoryUrl: String = "http://repo1.maven.org/maven2/"
    public var dir: String = "./lib"

    public fun repository(name: String, url: String): MavenResolverRule {
        repositoryName = name
        repositoryUrl = url
        return this
    }

    public fun invoke(body: MavenResolverRule.() -> Unit): MavenResolverRule {
        body()
        return this
    }
}


public open class MavenTool() : komplex.Resolver("maven") {

    override fun resolve(context: BuildStepContext, sources: Iterable<komplex.Artifact>, rule: ResolveTool.Rule): BuildResult = null!!

    internal fun resolveMaven(context: BuildStepContext, sources: Iterable<komplex.Artifact>, rule: MavenResolverRule): BuildResult {
        fun resput(scenario: Scenario, source: komplex.Artifact): komplex.Artifact {
            val res = rule.dryResolve(scenario, source)
            rule.source2target.put(source, res)
            return res
        }

        val remoteRepos: MutableList<RemoteRepository> = Arrays.asList(RemoteRepository(rule.repositoryName, "default", rule.repositoryUrl))
        val localRepo: File = File(rule.dir)
        // \todo error handling
        for (it in sources) {
            // \todo switch to regular logging
            if (it !is LibraryReferenceArtifact)
                return BuildResult.Fail
            val id = it.mavenId()
            println("[INFO] resolving [${it.id}]")
            val target = (rule.source2target.get(it) ?: resput(context.scenario, it)) as LibraryWithDependenciesArtifact
            val deps = Aether(remoteRepos, localRepo).resolve(
                    DefaultArtifact("${id.groupId}:${id.artifactId}:${id.version}"),
                    JavaScopes.RUNTIME)
            target.resolvedPaths = deps?.map { Paths.get(it.getFile().path) } ?: listOf()
            println("[INFO] resolved successfuly:")
            target.print("    ")
        }
        return BuildResult.Success
    }
}
