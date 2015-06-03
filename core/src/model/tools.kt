
package komplex.model

import komplex.utils.BuildDiagnostic
import komplex.utils.Named
import kotlin.properties.Delegates


public interface LambdaStep : Step {
    public val func: (Iterable<Pair<ArtifactDesc, ArtifactData?>>, Iterable<ArtifactDesc>) -> Iterable<ArtifactData>

    public override fun execute(context: BuildContext, artifacts: Map<ArtifactDesc, ArtifactData?>) : BuildResult {
        try {
            val res = func(sources.map { Pair(it, artifacts.get(it)) }, targets)
            assert(targets.count() == res.count())
            return BuildResult(BuildDiagnostic.Success, targets.zip(res))
        }
        catch (e: Exception) {
            // \todo add exception to the build result
            return BuildResult(BuildDiagnostic.Fail(e.toString()))
        }
    }
}


public interface Tool<Config> : Named {
    public fun execute(context: BuildContext,
                       cfg: Config,
                       src: Iterable<Pair<ArtifactDesc, ArtifactData?>>,
                       tgt: Iterable<ArtifactDesc>)
            : BuildResult
}


public interface ToolStep<Config, T: Tool<Config>> : Step {
    public val tool: T
    public val config: Config

    public override fun execute(context: BuildContext, artifacts: Map<ArtifactDesc, ArtifactData?>) : BuildResult {
        try {
            return tool.execute(context, config, sources.map { Pair(it, artifacts.get(it)) }, targets)
        }
        catch (e: Exception) {
            return BuildResult(BuildDiagnostic.Fail(e.toString()))
        }
    }
}


public open class LazyTool<Config, T: Tool<Config>>(
        override val name: String,
        val gen: () -> T
) : Tool<Config> {
    protected val tool: T by Delegates.lazy { gen() }
    override fun execute(context: BuildContext, cfg: Config, src: Iterable<Pair<ArtifactDesc, ArtifactData?>>, tgt: Iterable<ArtifactDesc>): BuildResult =
        tool.execute(context, cfg, src, tgt)
}


