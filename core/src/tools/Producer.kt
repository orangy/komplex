package komplex

public trait ProducingTool : Tool {

    public trait Rule : Tool.Rule {
        override val tool: ProducingTool
        val selectTargets: SelectArtifactsListTrait
        override fun targets(scenario: Scenario): Iterable<Artifact> = selectTargets.get(scenario)
        override fun execute(context: BuildStepContext): BuildResult = tool.produce(context, targets(context.scenario))
    }
    override fun execute(context: BuildStepContext, rule: Tool.Rule): BuildResult = produce(context, rule.sources(context.scenario))
    protected fun produce(context: BuildStepContext, to: Iterable<Artifact>): BuildResult
}


public abstract class Producer(override val title: String) : ProducingTool {
    public class Rule(override val tool: ProducingTool, override val export: Boolean = false) : ProducingTool.Rule {
        override val selectTargets = SelectArtifactsList()
    }
}


public fun <T : ProducingTool.Rule> T.into(vararg selectArtifacts: (scenario: Scenario) -> Iterable<Artifact>): T {
    selectTargets.add(*selectArtifacts)
    return this
}
/*
public fun <T : ProducingTool.Rule> T.into(vararg artifacts: Iterable<Artifact>): T {
    selectTargets.add(*artifacts)
    return this
}
*/
public fun <T : ProducingTool.Rule> T.into(vararg artifacts: Artifact): T {
    selectTargets.add(*artifacts)
    return this
}

