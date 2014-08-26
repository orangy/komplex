package komplex

public class ModuleScenario(val parent: ModuleScript, val scenarios: List<ScenarioSelector>) {
    val rules = arrayListOf<ModuleRule>()

    public fun invoke(body : ModuleScenario.()->Unit) {
        body()
    }

    public fun using(tool: Tool): ModuleRule {
        val process = ModuleRule(this, tool)
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

