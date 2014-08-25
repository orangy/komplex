package komplex

public class ModuleScenarios(val module: Module) {
    val scenarios = arrayListOf<ModuleScenarioRules>()

    public fun using(tool: Tool): ModuleScenarioRule = invoke(Scenario("*")).using(tool)

    public fun invoke(vararg scenario: Scenario, body: ModuleScenarioRules.() -> Unit): ModuleScenarioRules {
        val buildScenario = invoke(*scenario)
        buildScenario.body()
        return buildScenario
    }

    public fun invoke(vararg config: Scenario): ModuleScenarioRules {
        val build = ModuleScenarioRules(this, config.toList())
        scenarios.add(build)
        return build
    }

    fun dump(indent: String = "") {
        for (builds in scenarios) {
            builds.dump(indent)
        }
    }
}