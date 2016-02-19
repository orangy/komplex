
package komplex.dsl

import komplex.model.ScenarioSelector
import komplex.model.Scenarios
import komplex.model.combine

data class Scenario(val name: String) : komplex.model.Scenario {
    override fun toString(): String {
        return "Scenario $name"
    }
}

fun makeSelector(vararg scenarioNames: String) : ScenarioSelector =
    ScenarioSelector(Scenarios(scenarioNames.map { Scenario(it) }.distinct()))

fun makeSelector(vararg selectors: ScenarioSelector) : ScenarioSelector =
    if (selectors.isEmpty()) ScenarioSelector.None
    else selectors.reduce { a, b -> a.combine(b) }

fun scenario(name: String, body: ScenarioSelector.() -> Unit = {}): ScenarioSelector {
    val selector = makeSelector(name)
    selector.body()
    return selector
}

fun scenarios(vararg items: Scenario): Scenarios = Scenarios(items.asIterable())
fun scenarios(vararg items: ScenarioSelector): Scenarios = items.asIterable().combine().scenarios

