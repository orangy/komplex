
package komplex.dsl

import komplex.model.combine
import komplex.model.ScenarioSelector
import komplex.model.Scenarios

public class ModuleDependency(
        override val module: Module,
        override var scenarios: komplex.model.Scenarios,
        override var selector: ScenarioSelector
) : komplex.model.ModuleDependency {}


public open class DependencyGroup(val selectors: Iterable<ScenarioSelector>) {
    val items: MutableList<ModuleDependency> = arrayListOf()

    public fun on(vararg ms: Module): DependencyGroup {
        ms.forEach { items.add(ModuleDependency(it, Scenarios.Same, selectors.combine())) }
        return this
    }

    public fun on(vararg ms: Iterable<Module>): DependencyGroup {
        ms.forEach { it.forEach { items.add(ModuleDependency(it, Scenarios.Same, selectors.combine())) } }
        return this
    }

    public fun invoke(scenario: ScenarioSelector): Unit {
        items.forEach { it.scenarios = scenario.scenarios }
    }
}


public class Dependencies(vararg scenarios: ScenarioSelector = array(ScenarioSelector.Any)) : DependencyGroup(scenarios.toList()) {
    val groups: MutableList<DependencyGroup> = arrayListOf(this)

    inline public fun invoke(vararg scenarios: ScenarioSelector, body: DependencyGroup.() -> Unit): DependencyGroup {
        val group = invoke(*scenarios)
        group.body()
        return group
    }

    public fun invoke(vararg scenarios: ScenarioSelector): DependencyGroup {
        val group = DependencyGroup(scenarios.toList())
        groups.add(group)
        return group
    }

    public val modules: Iterable<ModuleDependency>
        get() = groups.flatMap { it.items }

}


