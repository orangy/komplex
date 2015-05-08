
package komplex.dsl

import komplex.model.ArtifactDesc
import komplex.model.combine
import komplex.model.ScenarioSelector
import komplex.model.Scenarios

public class ModuleDependency(
        override val module: Module,
        override var scenarios: komplex.model.Scenarios,
        override var selector: ScenarioSelector
) : komplex.model.ModuleDependency {
    override fun toString(): String = "${module.name} (${selector.scenarios})"
}


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


public class Dependencies(vararg scenarios: ScenarioSelector = arrayOf(ScenarioSelector.Any)) : DependencyGroup(scenarios.toList()) {
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

    public val modules: Iterable<komplex.model.ModuleDependency>
        get() = groups.flatMap { it.items }

}

public fun Dependencies.allArtifacts(scenarios: Scenarios = Scenarios.Same): Iterable<ArtifactDesc> =
        modules.flatMap { it.module.targets(scenarios) }
