
package komplex.dsl

import komplex.log
import komplex.model.ModuleMetadata
import komplex.model.ScenarioSelector
import komplex.model.Scenarios
import komplex.model.Step
import komplex.utils.BuildDiagnostic
import komplex.utils.plus
import java.nio.file.Path


interface ConfigurablesCollection {

    // filled with configurables (steps and submodules) in the order of adding.
    // NOTE - order is important for proper dependency-related configuration, so separate collection is used
    val configurableChildren: MutableCollection<Configurable>
    var configurationDiagnostic: BuildDiagnostic

    fun addConfigurable(c: Any): Boolean = if (c is Configurable) configurableChildren.add(c) else false
    fun configureChildren(): Boolean {
        val unconfigured = configurableChildren.filterNot { it.configured }
        if (unconfigured.none()) return false
        configurationDiagnostic = unconfigured.fold( configurationDiagnostic, { r, c -> r + c.configure() })
        return true
    }
}


// the name is misleading, it is now contains not only modules but also all other configurables,
// and it also services as a validation entry point
// \todo rename or reorganise hierarchy and/or aggregation
open class ModuleCollection(override val parent: ProjectModule? = null) : komplex.model.ModuleCollection, ScriptContext, Configurable, Validable, ConfigurablesCollection {

    override val env: ContextEnvironment = ContextEnvironment(parent)
    override val children: MutableList<komplex.model.Module> = arrayListOf()
    override var configured: Boolean = false
    override val configurableChildren: MutableCollection<Configurable> = arrayListOf()
    override var configurationDiagnostic: BuildDiagnostic = BuildDiagnostic.Success

    fun module(name: String, description: String? = null, rootPath: Path? = null, body: ProjectModule.() -> Unit): ProjectModule {
        // first configure all known to this point configurables
        configureChildren()
        val module = ProjectModule(this as? ProjectModule, name, rootPath)
        if (description != null)
            module.description(description)
        module.body()
        children.add(module)
        addConfigurable(module)
        return module
    }

    override fun configure(): BuildDiagnostic {
        if (!configured) {
            configureChildren()
            configured = true
        }
        return configurationDiagnostic
    }

    // validates module before usage, intended to be called after configuration
    override fun validate(): BuildDiagnostic =
            // validate children (submodules)
            children.filter { it is Validable }.fold( BuildDiagnostic.Success, { r, module -> r + module.validateIfValidable() })
}


// not a nice name, need something more precise, may be different hierarchy altogether. The idea is that dsl module is mutable entity
// on top of some more specific (than in komplex.model) model entity describing build module, e.g. in typical for JVM world sense
// \todo rename/reorganise hierarchy
open class ProjectModule(parent1: ProjectModule?, override val name: String, rootPath: Path? = null)
: ModuleCollection(parent1), komplex.model.Module, GenericSourceType {

    val moduleName : String get() = name

    open class Metadata : ModuleMetadata {
        internal var description: String? = null
        internal var version: String? = null
    }

    override val metadata: Metadata = Metadata()
    override val dependencies: Iterable<komplex.model.ConditionalModuleDependency> get() = depends.modules.coll

    override val steps: Iterable<Step>
        get() = ruleSets.flatMap { it.rules }

    val title: String
        get() = "$name${if (version.isNullOrEmpty()) "" else "-$version"}${if (description.isNullOrEmpty()) "" else " ($description)"}"

    var version: String?
        get() = metadata.version ?: parent?.version
        set(value: String?) { metadata.version = value }

    val rootPath: Path by lazy { (rootPath ?: throw IllegalArgumentException("Module root path is not defined")).normalize().toAbsolutePath() }

    val ruleSets: MutableList<ModuleRuleSet> = arrayListOf()

    fun version(value: String) {
        version = value
    }

    var description: String?
        get() = metadata.description
        set(value: String?) { metadata.description = value }

    fun description(value: String) {
        metadata.description = value
    }

    val depends: Dependencies = Dependencies()
    val build: ModuleRuleSet get() {
        // first configure all known to this point configurables
        configureChildren()
        val rs = ModuleRuleSet(this)
        ruleSets.add(rs)
        return rs
    }

    override fun toString(): String = "$title"
    override var defaultScenario: komplex.model.Scenarios = Scenarios.All

    // \todo consider making it a method returning final immutable module
    // intended to be called after creation and manual configuration, but before validation, could be used for automated configuration, e.g. export
    override fun configure(): BuildDiagnostic {
        var res = BuildDiagnostic.Success
        if (configured) return res

        log.debug("configuring $fullName")

        res = super.configure()

        // collect all artifacts that are consumed within module itself
        val consumed = steps.flatMap { it.sources }.toHashSet()

        // then for each rule, if the rule's targets are not consumed internally, assuming it is an export one
        steps.forEach { step ->
            if (!step.export && step is Rule && step.targets.none { consumed.contains(it) }) {
                step.export = true
                log.debug("setting $step as export")
            }
        }
        // steps configuration happens in the parent method call, so steps can use autoconfigured export flag
        return res
    }

    // validates module before usage, intended to be called after configuration
    override fun validate(): BuildDiagnostic =
            // validate children (submodules) and then steps
            steps.fold(
                    super.validate(),
                    { r, step -> if (step is Rule) r + step.validateIfValidable() else r })
}

fun ProjectModule.default(scenario: ScenarioSelector) : ProjectModule {
    defaultScenario = scenario.scenarios
    return this
}

