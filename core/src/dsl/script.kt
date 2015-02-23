
package komplex.dsl

import komplex.model.BuildGraph

/*
public class ModuleScript(val module: Module) {
    public val ruleSets: MutableList<ModuleRuleSet> = arrayListOf()

    public fun using<TR : Rule>(rule: TR): TR = invoke(ScenarioSelector("*")).using(rule)

    inline public fun invoke(vararg scenario: ScenarioSelector, body: ModuleRuleSet.() -> Unit): ModuleRuleSet {
        val ruleSet = invoke(*scenario)
        ruleSet.body()
        return ruleSet
    }

    public fun invoke(vararg config: ScenarioSelector): ModuleRuleSet {
        val ruleSet = ModuleRuleSet(this, config.toList())
        ruleSets.add(ruleSet)
        return ruleSet
    }
}
*/

public fun script(body: BuildScript.() -> Unit): BuildScript {
    val script = BuildScript()
    script.body()
    return script
}


public class BuildScript() : ModuleCollection() {

    fun buildGraph(): BuildGraph {
        val graph = BuildGraph()
        children.forEach { graph.add(it) }
        return graph
    }
}
