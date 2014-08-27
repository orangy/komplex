package komplex

import java.util.ArrayList

public class ModuleScenario(val parent: ModuleScript, val scenarios: List<ScenarioSelector>) {
    public val rules: MutableList<ModuleRule> = arrayListOf<ModuleRule>()

    public fun invoke(body: ModuleScenario.() -> Unit) {
        body()
    }

    public fun using<TTool : Tool>(tool: TTool): TTool {
        val rule = ModuleToolRule(this, tool)
        rules.add(rule)
        return tool
    }

    public fun using<TTool : Tool>(tool: TTool, body: TTool.() -> Unit): TTool {
        tool.body()
        val rule = ModuleToolRule(this, tool)
        rules.add(rule)
        return tool
    }
}

