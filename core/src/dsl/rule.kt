
package komplex.dsl

import komplex.model.*
import komplex.utils.BuildDiagnostic

// rules in fact
object tools {}

// \todo - redesign the rules/tools/steps to less inheritance and with more aggregation and traits
// \todo optimize with separating Step and Rule, so rule could be mutable, but converted into immutable Step then added to the graph,
// or alternatively make rule immutable with copy-on-write behaviour

interface Configurable {
    var configured: Boolean
    // intended to be called after creation and manual configuration, but before validation, could be used for automated configuration, e.g. target choosing
    // \todo consider making it a method returning final immutable step
    open fun configure(): BuildDiagnostic {
        configured = true
        return BuildDiagnostic.Success
    }
}

fun Any.configureIfConfigurable(): BuildDiagnostic = (this as? Configurable)?.configure() ?: BuildDiagnostic.Success


interface Validable {
    // validates step before usage, intended to be called after configuration
    open fun validate(): BuildDiagnostic = BuildDiagnostic.Success
}

fun Any.validateIfValidable(): BuildDiagnostic = (this as? Validable)?.validate() ?: BuildDiagnostic.Success


class RuleSources {
    val artifacts: MutableCollection<Artifact> = arrayListOf()
    val modules: MutableCollection<Module> = arrayListOf()
    val moduleDependencies: MutableCollection<ModuleDependency> = arrayListOf()
    val rules: MutableCollection<Rule> = arrayListOf()

    // \todo this is an optimization point, see todo above
    fun collect(scenarios: Scenarios): Iterable<ArtifactDesc> =
            artifacts +
            modules.flatMap { it.targets(scenarios) } +
            moduleDependencies.flatMap { it.targets(scenarios) } +
            rules.filter { scenarios.matches(it.selector) }.flatMap { it.targets }
}


interface Rule : Step, GenericSourceType, Configurable, Validable {

    var module: ProjectModule
    val explicitFroms: RuleSources
    val explicitDepends: RuleSources
    val explicitTargets: MutableCollection<Artifact> // filled with "into" and "export"

    val fromSources: Iterable<ArtifactDesc> get() = explicitFroms.collect(selector.scenarios)
    val dependsSources: Iterable<ArtifactDesc> get() = explicitDepends.collect(selector.scenarios)

    override var selector: ScenarioSelector

    override val sources: Iterable<ArtifactDesc> get() = fromSources + dependsSources
    override var export: Boolean
    override val targets: Iterable<ArtifactDesc> get() = explicitTargets

    // validates step before usage, intended to be called after configuration
    override  fun validate(): BuildDiagnostic {
        val msgs = arrayListOf<String>()
        val intersection = sources.intersect(targets)
        if (intersection.any())
            msgs.add("$name (${module.fullName}) both sources and targets contain (${intersection.map { it.name }.joinToString(", ")})")
        if (export && targets.none()) msgs.add("$name (${module.fullName}) export step should have targets")
        return if (msgs.any()) BuildDiagnostic.Fail(msgs) else BuildDiagnostic.Success
    }
}


abstract class RuleImpl : Rule {
    private var module_: ProjectModule? = null
    override var module: ProjectModule
        get() = module_!!
        set(v: ProjectModule) { if (module_!=null) throw Exception("Module ${module_!!.fullName} already set for $name"); module_ = v }
    override val explicitFroms: RuleSources = RuleSources()
    override val explicitDepends: RuleSources = RuleSources()
    override val explicitTargets: MutableCollection<Artifact> = arrayListOf()
    override var selector: ScenarioSelector = ScenarioSelector.Any
    override var export: Boolean = false
    override var configured: Boolean = false
}


infix fun <TR : Rule> TR.with(body: TR.() -> Unit): TR {
    body()
    return this
}


// the usage of GenericSourceType should improve type safety in script, but prevent using Iterable directly, it should now
// be wrapped into something "implementing" GenericSourceType, see e.g. ModuleDependencies
// \todo may be this will not be flexible enough, consider some other variants

fun <T : Rule, S: GenericSourceType> T.addToSources(sources: RuleSources, vararg args: S): T = addToSources(sources, args.asIterable())

fun <T : Rule, S: GenericSourceType> T.addToSources(sources: RuleSources, args: Iterable<S>): T {
    for (arg in args)
        when (arg) {
            is ArtifactsSet -> sources.artifacts.addAll(arg.members)
            is Array<*> -> addToSources(sources, arg.asIterable() as Iterable<GenericSourceType>)
            is Iterable<*> -> addToSources(sources, arg as Iterable<GenericSourceType>)
            is Artifact -> sources.artifacts.add(arg)
            is ModuleDependency -> sources.moduleDependencies.add(arg)
            is ModuleDependencies -> addToSources(sources, arg.coll as Iterable<ModuleDependency>)
            is Module -> sources.modules.add(arg)
            is Rule -> sources.rules.add(arg)
            is RuleSetDesc -> sources.rules.addAll(arg.rules)
            else -> throw Exception("Unknown source type: $arg")
        }
    return this
}

infix fun <T : Rule, S: GenericSourceType> T.from(args: Iterable<S>): T = addToSources(explicitFroms, args)
fun <T : Rule, S: GenericSourceType> T.from(vararg args: S): T = addToSources(explicitFroms, *args)
infix fun <T : Rule, S: GenericSourceType> T.from(arg: S): T = addToSources(explicitFroms, arg)

fun <T : Rule, S: GenericSourceType> T.dependsOn(args: Iterable<S>): T = addToSources(explicitDepends, args)
fun <T : Rule, S: GenericSourceType> T.dependsOn(vararg args: S): T = addToSources(explicitDepends, *args)

fun <T : Rule> T.into(vararg artifacts: Iterable<Artifact>): T {
    artifacts.forEach { explicitTargets.addAll(it) }
    return this
}

fun <T : Rule> T.into(vararg artifacts: Artifact): T {
    explicitTargets.addAll(artifacts)
    return this
}

infix fun <T : Rule> T.into(artifact: Artifact): T {
    explicitTargets.add(artifact)
    return this
}

// \todo: find better way to export
fun <T : Rule> T.export(vararg artifacts: Iterable<Artifact>): T {
    export = true
    return into(*artifacts)
}

fun <T : Rule> T.export(vararg artifacts: Artifact): T {
    export = true
    return into(*artifacts)
}


class LambdaRule(
       var body: ((Iterable<Pair<ArtifactDesc, ArtifactData?>>, Iterable<ArtifactDesc>) -> Iterable<ArtifactData>)? = null
) : RuleImpl(), LambdaStep {

    companion  object {
        internal var counter = 0;
    }
    override var name: String = "lambda${++counter}"

    override val func: (Iterable<Pair<ArtifactDesc, ArtifactData?>>, Iterable<ArtifactDesc>) -> Iterable<ArtifactData>
        get() = body!!
}


fun LambdaRule.invoke(f: (Iterable<Pair<ArtifactDesc, ArtifactData?>>, Iterable<ArtifactDesc>) -> Iterable<ArtifactData>): LambdaRule {
    body = f
    return this
}


open class BasicToolRule<Config, T: Tool<Config>>(
        override val tool: T
) : RuleImpl(), ToolStep<Config, T> {
    override val config: Config = this as Config

    override var name: String = tool.name
    override val sources: Iterable<ArtifactDesc> get() = super.sources
}


val tools.custom: LambdaRule get() = LambdaRule()
fun tools.custom( body: ((Iterable<Pair<ArtifactDesc, ArtifactData?>>, Iterable<ArtifactDesc>) -> Iterable<ArtifactData>)): LambdaRule = LambdaRule(body)

