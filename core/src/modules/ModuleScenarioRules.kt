package komplex

public class ModuleScenarioRules(val buildModule: ModuleScenarios, val scenarios: List<Scenario>) {
    val rules = arrayListOf<ModuleScenarioRule>()

    public fun invoke(body : ModuleScenarioRules.()->Unit) {
        body()
    }

    public fun using(tool: Tool): ModuleScenarioRule {
        val process = ModuleScenarioRule(this, tool)
        rules.add(process)
        return process
    }

    fun dump(indent: String = "") {
        println("$indent Build $scenarios")
        for (rule in rules) {
            rule.dump(indent + "  ")
        }
    }
}

