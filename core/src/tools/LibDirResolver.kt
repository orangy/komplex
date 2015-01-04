
package komplex

import java.nio.file.Paths
import java.nio.file.Files

// \todo add Either<file> -like artifact and use it for resolving

public val tools.libDir: LibDirResolver.Rule
    get() = LibDirResolver.Rule()


public class LibDirResolver : Resolver("localDirResolver") {

    public class Rule(public var dir: String = "./lib", override val local: Boolean = false) : Resolver.BaseRule(local) {
        override val tool: LibDirResolver = LibDirResolver()
        override fun dryResolve(scenario: Scenario, source: Artifact): Artifact =
            when (source) {
                is LibraryReferenceArtifact -> SingleLibraryArtifact(source, dir)
                // \todo design better error reporting for construction phase
                else -> throw Exception("Unknown artifact type")
        }
        override fun execute(context: BuildStepContext): BuildResult = tool.resolveInDir(context, sources(context.scenario), this)
    }

    override fun resolve(context: BuildStepContext, sources: Iterable<Artifact>, rule: ResolveTool.Rule): BuildResult = null!!

    protected fun resolveInDir(context: BuildStepContext, sources: Iterable<Artifact>, rule: LibDirResolver.Rule): BuildResult {
        for (s in sources) {
            when (s) {
                // \todo handle dependencies
                // \todo failure diagnostics
                is LibraryReferenceArtifact -> {
                    val lib = SingleLibraryArtifact(s, rule.dir)
                    if (!Files.exists(lib.path)) return BuildResult.Fail
                }
                else -> return BuildResult.Fail
            }
        }
        return BuildResult.Success
    }
}
