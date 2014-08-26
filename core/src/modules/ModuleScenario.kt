package komplex

public class ModuleScenario(val parent: ModuleScript, val scenarios: List<ScenarioSelector>) {
    val rules = arrayListOf<ModuleRule>()

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

    fun dump(indent: String = "") {
        println("$indent Scenario $scenarios")
        for (rule in rules) {
            rule.dump(indent + "  ")
        }
    }
}

