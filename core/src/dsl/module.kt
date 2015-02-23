
package komplex.dsl

import komplex.model.ModuleMetadata
import komplex.model.Step
import komplex.model.ScenarioSelector
import komplex.model.Scenarios

public open class ModuleCollection(override val parent: Module? = null) : komplex.model.ModuleCollection {
    override val children: MutableList<Module> = arrayListOf()

    public fun module(name: String, description: String? = null, body: Module.() -> Unit): Module {
        val module = Module(parent, name)
        if (description != null)
            module.description(description)
        module.body()
        children.add(module)
        return module
    }
}


public open class Module(parent1: Module?, override val name: String) : ModuleCollection(parent1), komplex.model.Module {
    public val moduleName : String get() = name

    public open class Metadata : ModuleMetadata {
        internal var description: String = ""
        internal var version: String = ""
    }

    override val metadata: Metadata = Metadata()
    override val dependencies: MutableList<ModuleDependency> = arrayListOf()

    override val steps: Iterable<Step>
        get() = ruleSets.flatMap { it.rules }

    public val title: String
        get() = if (description.isEmpty()) name else "$name ($description)"

    public var version: String
        get() = metadata.version
        set(value: String) { metadata.version = value }

    public val ruleSets: MutableList<ModuleRuleSet> = arrayListOf()

    public fun version(value: String) {
        metadata.version = value
    }

    public var description: String
        get() = metadata.description
        set(value: String) { metadata.description = value }

    public fun description(value: String) {
        metadata.description = value
    }

    public val depends: Dependencies = Dependencies()
    public val build: ModuleRuleSet get() {
        val rs = ModuleRuleSet(this)
        ruleSets.add(rs)
        return rs
    }

    override fun toString(): String = "$title"
    override var defaultScenario: komplex.model.Scenarios = Scenarios.All
}

public fun Module.default(scenario: ScenarioSelector) : Module {
    defaultScenario = scenario.scenarios
    return this
}
