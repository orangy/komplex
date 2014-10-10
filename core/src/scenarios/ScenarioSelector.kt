package komplex

public class ScenarioSelector(val pattern: String) {
    val regex = pattern.replace("?", ".").replace("*", ".*")

    fun matches(scenario: Scenario): Boolean = scenario.name.matches(regex)

    override fun toString(): String {
        return pattern
    }
}

public fun scenario(name: String, body: ScenarioSelector.() -> Unit = {}): ScenarioSelector {
    val scenario = ScenarioSelector(name)
    scenario.body()
    return scenario
}
