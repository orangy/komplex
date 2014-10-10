package komplex

/**
 * Collection of (scenarios)[Scenario] for the [module]
 */
public class ModuleScript(val module: Module) {
    public val scenarios: MutableList<ModuleScenario> = arrayListOf()

    public fun using<TTool : Tool>(tool: TTool): TTool = invoke(ScenarioSelector("*")).using(tool)

    inline public fun invoke(vararg scenario: ScenarioSelector, body: ModuleScenario.() -> Unit): ModuleScenario {
        val buildScenario = invoke(*scenario)
        buildScenario.body()
        return buildScenario
    }

    public fun invoke(vararg config: ScenarioSelector): ModuleScenario {
        val build = ModuleScenario(this, config.toList())
        scenarios.add(build)
        return build
    }
}