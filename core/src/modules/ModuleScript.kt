package komplex

public class ModuleScript(val module: Module) {
    val scenarios = arrayListOf<ModuleScenario>()

    public fun using<TTool : Tool>(tool: TTool): TTool = invoke(ScenarioSelector("*")).using(tool)

    public fun invoke(vararg scenario: ScenarioSelector, body: ModuleScenario.() -> Unit): ModuleScenario {
        val buildScenario = invoke(*scenario)
        buildScenario.body()
        return buildScenario
    }

    public fun invoke(vararg config: ScenarioSelector): ModuleScenario {
        val build = ModuleScenario(this, config.toList())
        scenarios.add(build)
        return build
    }

    fun dump(indent: String = "") {
        for (builds in scenarios) {
            builds.dump(indent)
        }
    }
}