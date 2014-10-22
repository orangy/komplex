package komplex

import java.util.ArrayList

public class ModuleRuleSet(val parent: ModuleScript, val selectors: List<ScenarioSelector>) {
    public val rules: MutableList<ModuleRule> = arrayListOf()

    public fun invoke(body: ModuleRuleSet.() -> Unit) {
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

