
package komplex.dsl

import komplex.model.ModuleMetadata
import komplex.model.Step
import komplex.model.ScenarioSelector
import komplex.model.Scenarios
import java.nio.file.Path

public open class ModuleCollection(override val parent: Module? = null) : komplex.model.ModuleCollection, ScriptContext {
    override val env: ContextEnvironment = ContextEnvironment(parent)
    override val children: MutableList<Module> = arrayListOf()

    public fun module(name: String, description: String? = null, body: Module.() -> Unit): Module {
        val module = Module(this as? Module, name)
        if (description != null)
            module.description(description)
        module.body()
        children.add(module)
        return module
    }
}


public open class Module(parent1: Module?, override val name: String) : ModuleCollection(parent1), komplex.model.Module, GenericSourceType {
    public val moduleName : String get() = name

    public open class Metadata : ModuleMetadata {
        internal var description: String? = null
        internal var version: String? = null
    }

    override val metadata: Metadata = Metadata()
    override val dependencies: Iterable<komplex.model.ModuleDependency> get() = depends.modules.coll

    override val steps: Iterable<Step>
        get() = ruleSets.flatMap { it.rules }

    public val title: String
        get() = "$name${if (version.isNullOrEmpty()) "" else "-$version"}${if (description.isNullOrEmpty()) "" else " ($description)"}"

    public var version: String?
        get() = metadata.version ?: parent?.version
        set(value: String?) { metadata.version = value }

    public val ruleSets: MutableList<ModuleRuleSet> = arrayListOf()

    public fun version(value: String) {
        version = value
    }

    public var description: String?
        get() = metadata.description
        set(value: String?) { metadata.description = value }

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

