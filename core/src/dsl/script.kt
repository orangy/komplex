
package komplex.dsl

import komplex.model.BuildGraph
import komplex.model.Scenarios
import komplex.utils.BuildDiagnostic
import komplex.utils.plus
import java.nio.file.Path
import java.nio.file.Paths

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

    val configured = script.configure()
    if (configured.status != BuildDiagnostic.Status.Succeeded)
        throw Exception("Failed to configure script:\n  " + configured.messages.joinToString("\n  "))
    val valid = script.validate()
    if (valid.status != BuildDiagnostic.Status.Succeeded)
        throw Exception("Failed to validate script:\n  " + valid.messages.joinToString("\n  "))
    return script
}


public class BuildScript() : ModuleCollection(), ScriptContext {

    init {
        env.rootDir = Paths.get(".")
    }

    fun buildGraph(): BuildGraph {
        val graph = BuildGraph()
        children.forEach { module -> graph.add(module) }
        return graph
    }
}
