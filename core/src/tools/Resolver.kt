
package komplex

import komplex.Tool
import java.util.HashMap

public trait ResolveTool : ConsumingTool {
    public trait Rule : ConsumingTool.Rule {
        override val tool: ResolveTool
        val source2target: MutableMap<Artifact, Artifact>
        override fun targets(scenario: Scenario): Iterable<Artifact> {
            // \todo optimize - iterate only if sources or scenario are changed
            sources(scenario).map {
                if (!source2target.containsKey(it))
                    source2target.put(it, dryResolve(scenario, it))
            }
            return source2target.values()
        }
        override fun execute(context: BuildStepContext): BuildResult
                = tool.resolve(context, sources(context.scenario), this)
        public fun dryResolve(scenario: Scenario, source: Artifact): Artifact
    }

    override fun execute(context: BuildStepContext, rule: Tool.Rule): BuildResult = null!!
    override fun consume(context: BuildStepContext, from: Iterable<Artifact>): BuildResult = null!!
    protected fun resolve(context: BuildStepContext, sources: Iterable<Artifact>, rule: Rule): BuildResult
}


public abstract class Resolver(public override val title: String) : ResolveTool {
    public abstract class BaseRule(override val local: Boolean): ResolveTool.Rule {
        override val selectSources = SelectArtifactsList()
        override val source2target = hashMapOf<Artifact, Artifact>()
    }
}

