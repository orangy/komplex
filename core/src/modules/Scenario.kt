package komplex

public fun scenario(name: String, body: ScenarioSelector.() -> Unit = {}): ScenarioSelector {
    val scenario = ScenarioSelector(name)
    scenario.body()
    return scenario
}

public class Scenario(val name: String) {
    override fun toString(): String {
        return "Scenario $name"
    }
}

public class ScenarioSelector(val pattern: String) {
    val regex = pattern.replace("?", ".").replace("*", ".*")

    fun matches(scenario: Scenario): Boolean = scenario.name.matches(regex)

    override fun toString(): String {
        return pattern
    }
}

