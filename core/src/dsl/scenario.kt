
package komplex.dsl

import komplex.model.ScenarioSelector
import komplex.model.Scenarios
import komplex.model.combine

public data class Scenario(val name: String) : komplex.model.Scenario {
    override fun toString(): String {
        return "Scenario $name"
    }
}

public fun makeSelector(vararg scenarioNames: String) : ScenarioSelector =
    ScenarioSelector(Scenarios(scenarioNames.map { Scenario(it) }.distinct()))

public fun makeSelector(vararg selectors: ScenarioSelector) : ScenarioSelector =
    if (selectors.isEmpty()) ScenarioSelector.None
    else selectors.reduce { a, b -> a.combine(b) }

public fun scenario(name: String, body: ScenarioSelector.() -> Unit = {}): ScenarioSelector {
    val selector = makeSelector(name)
    selector.body()
    return selector
}

public fun scenarios(vararg items: Scenario): Scenarios = Scenarios(items.asIterable())
public fun scenarios(vararg items: ScenarioSelector): Scenarios = items.asIterable().combine().scenarios

