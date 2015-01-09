package komplex

public trait ConvertingTool : ProducingTool, ConsumingTool {

    public trait Rule : ProducingTool.Rule, ConsumingTool.Rule {
        override val tool: ConvertingTool
        override fun execute(context: BuildStepContext): BuildResult
                = tool.convert(context, sources(context.scenario), targets(context.scenario))
    }

    override fun execute(context: BuildStepContext, rule: Tool.Rule): BuildResult = null!!
    override fun produce(context: BuildStepContext, to: Iterable<Artifact>): BuildResult = null!!
    override fun consume(context: BuildStepContext, from: Iterable<Artifact>): BuildResult = null!!
    protected fun convert(context: BuildStepContext, from: Iterable<Artifact>, to: Iterable<Artifact>): BuildResult
}

public abstract class Converter(public override val title : String) : ConvertingTool {
    public abstract class BaseRule(override val export: Boolean = false) : ConvertingTool.Rule {
        override val selectSources = SelectArtifactsList()
        override val selectTargets = SelectArtifactsList()
    }
    public class Rule(override val tool: ConvertingTool, override val export: Boolean = false) : BaseRule(export) {}
}
