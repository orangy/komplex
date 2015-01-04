package komplex

public trait CompilerTool : ConvertingTool {

    public trait Rule : ConsumingTool.Rule, ProducingTool.Rule {
        override val tool: CompilerTool
        val selectLibs: SelectArtifactsListTrait
        override fun sources(scenario: Scenario): Iterable<Artifact> {
            val res = selectSources.get(scenario).toArrayList()
            res.addAll(selectLibs.get(scenario))
            return res
        }
        override fun execute(context: BuildStepContext): BuildResult
                = tool.compile(context, selectSources.get(context.scenario), selectTargets.get(context.scenario), selectLibs.get(context.scenario))
    }
    override fun produce(context: BuildStepContext, to: Iterable<Artifact>): BuildResult = null!!
    override fun consume(context: BuildStepContext, from: Iterable<Artifact>): BuildResult = null!!
    override fun convert(context: BuildStepContext, from: Iterable<Artifact>, to: Iterable<Artifact>): BuildResult = null!!
    protected fun compile(context: BuildStepContext, from: Iterable<Artifact>, to: Iterable<Artifact>, useLibs: Iterable<Artifact>): BuildResult
}


public abstract class Compiler(public override val title : String) : CompilerTool {
    public abstract class BaseRule(override val local: Boolean = false) : CompilerTool.Rule {
        override val selectSources = SelectArtifactsList()
        override val selectTargets = SelectArtifactsList()
        override val selectLibs = SelectArtifactsList()
    }
}


public fun <T : CompilerTool.Rule> T.useLibs(vararg selectArtifacts: Iterable<(scenario: Scenario) -> Iterable<Artifact>>): T {
    selectLibs.add(*selectArtifacts)
    return this
}

public fun <T : CompilerTool.Rule> T.useLibs(vararg selectArtifacts: (scenario: Scenario) -> Iterable<Artifact>): T {
    selectLibs.add(*selectArtifacts)
    return this
}
/*
public fun <T : CompilerTool.Rule> T.useLibs(vararg artifacts: Iterable<Artifact>): T {
    selectLibs.add(*artifacts)
    return this
}
*/
public fun <T : CompilerTool.Rule> T.useLibs(vararg artifacts: Artifact): T {
    selectLibs.add(*artifacts)
    return this
}

public fun <T : CompilerTool.Rule> T.useLibs(vararg rules: Tool.Rule): T {
    selectLibs.add(*rules)
    return this
}
