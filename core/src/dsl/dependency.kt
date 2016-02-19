
package komplex.dsl

import komplex.model.*

class ModuleDependency(
        override val module: Module,
        override var scenarios: komplex.model.Scenarios,
        override var selector: ScenarioSelector
) : komplex.model.ConditionalModuleDependency, GenericSourceType {
    override fun toString(): String = "${module.name} (${selector.scenarios})"
}

// need it in order to make the collection a GenericSourceType
class ModuleDependencies(val coll: Iterable<komplex.model.ConditionalModuleDependency>): GenericSourceType {}


open class DependencyGroup(val selectors: Iterable<ScenarioSelector>) {
    val items: MutableList<ModuleDependency> = arrayListOf()

    fun on(vararg ms: Module): DependencyGroup {
        ms.forEach { addModuleDependency(it) }
        return this
    }

    infix fun on(module: Module): DependencyGroup {
        addModuleDependency(module)
        return this
    }

    fun on(vararg ms: Iterable<Module>): DependencyGroup {
        ms.forEach { it.forEach { addModuleDependency(it) } }
        return this
    }

    infix fun on(ms: Iterable<Module>): DependencyGroup {
        ms.forEach { addModuleDependency(it) }
        return this
    }

    fun invoke(scenario: ScenarioSelector): Unit {
        items.forEach { it.scenarios = scenario.scenarios }
    }

    private fun addModuleDependency(module: Module) {
        items.add(ModuleDependency(module, Scenarios.Same, selectors.combine()))
    }
}


class Dependencies(vararg scenarios: ScenarioSelector = arrayOf(ScenarioSelector.Any)) : DependencyGroup(scenarios.toList()) {
    val groups: MutableList<DependencyGroup> = arrayListOf(this)

    inline fun invoke(vararg scenarios: ScenarioSelector, body: DependencyGroup.() -> Unit): DependencyGroup {
        val group = invoke(*scenarios)
        group.body()
        return group
    }

    fun invoke(vararg scenarios: ScenarioSelector): DependencyGroup {
        val group = DependencyGroup(scenarios.toList())
        groups.add(group)
        return group
    }

    val modules: ModuleDependencies
        get() = ModuleDependencies(groups.flatMap { it.items })

}

fun Dependencies.allArtifacts(scenarios: Scenarios = Scenarios.Same): Iterable<ArtifactDesc> =
        modules.coll.flatMap { it.module.targets(scenarios) }
