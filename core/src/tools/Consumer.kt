package komplex

public trait ConsumingTool : Tool {

    public trait Rule : Tool.Rule {
        override val tool: ConsumingTool
        val selectSources: SelectArtifactsListTrait
        override fun sources(scenario: Scenario): Iterable<Artifact> = selectSources.get(scenario)
        override fun execute(context: BuildStepContext): BuildResult = tool.consume(context, sources(context.scenario))
    }

    override fun execute(context: BuildStepContext, rule: Tool.Rule): BuildResult = null!!
    protected fun consume(context: BuildStepContext, from: Iterable<Artifact>): BuildResult
}


public abstract class Consumer(override val title: String) : ConsumingTool {
    public class Rule(override val tool: ConsumingTool, override val local: Boolean = false) : ConsumingTool.Rule {
        override val selectSources = SelectArtifactsList()
    }
}


public fun <T : ConsumingTool.Rule> T.from(vararg selectArtifacts: (scenario: Scenario) -> Iterable<Artifact>): T {
    selectSources.add(*selectArtifacts)
    return this
}
/*
public fun <T : ConsumingTool.Rule> T.from(vararg artifacts: Iterable<Artifact>): T {
    selectSources.add(*artifacts)
    return this
}
*/
public fun <T : ConsumingTool.Rule> T.from(vararg artifacts: Artifact): T {
    selectSources.add(*artifacts)
    return this
}

public fun <T : ConsumingTool.Rule> T.from(vararg rules: Tool.Rule): T {
    selectSources.add(*rules)
    return this
}
