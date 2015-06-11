
package komplex.dsl

import komplex.model.*
import komplex.utils.BuildDiagnostic

// rules in fact
public object tools {}

// \todo - redesign the rules/tools/steps to less inheritance and with more aggregation and traits
// \todo optimize with separating Step and Rule, so rule could be mutable, but converted into immutable Step then added to the graph,
// or alternatively make rule immutable with copy-on-write behaviour

public interface Configurable {
    public var configured: Boolean
    // intended to be called after creation and manual configuration, but before validation, could be used for automated configuration, e.g. target choosing
    // \todo consider making it a method returning final immutable step
    public open fun configure(): BuildDiagnostic {
        configured = true
        return BuildDiagnostic.Success
    }
}

public fun Any.configureIfConfigurable(): BuildDiagnostic = (this as? Configurable)?.configure() ?: BuildDiagnostic.Success


public interface Validable {
    // validates step before usage, intended to be called after configuration
    public open fun validate(): BuildDiagnostic = BuildDiagnostic.Success
}

public fun Any.validateIfValidable(): BuildDiagnostic = (this as? Validable)?.validate() ?: BuildDiagnostic.Success


public data class RuleSources {
    public val artifacts: MutableCollection<Artifact> = arrayListOf()
    public val modules: MutableCollection<Module> = arrayListOf()
    public val moduleDependencies: MutableCollection<ModuleDependency> = arrayListOf()
    public val rules: MutableCollection<Rule> = arrayListOf()

    // \todo this is an optimization point, see todo above
    public fun collect(scenarios: Scenarios): Iterable<ArtifactDesc> =
            artifacts +
            modules.flatMap { it.targets(scenarios) } +
            moduleDependencies.flatMap { it.targets(scenarios) } +
            rules.filter { scenarios.matches(it.selector) }.flatMap { it.targets }
}


public interface Rule : Step, GenericSourceType, Configurable, Validable {

    public var module: ProjectModule
    internal val explicitFroms: RuleSources
    internal val explicitDepends: RuleSources
    internal val explicitTargets: MutableCollection<Artifact> // filled with "into" and "export"

    public val fromSources: Iterable<ArtifactDesc> get() = explicitFroms.collect(selector.scenarios)
    public val dependsSources: Iterable<ArtifactDesc> get() = explicitDepends.collect(selector.scenarios)

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


public abstract class RuleImpl : Rule {
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


public fun <TR : Rule> TR.with(body: TR.() -> Unit): TR {
    body()
    return this
}


// the usage of GenericSourceType should improve type safety in script, but prevent using Iterable directly, it should now
// be wrapped into something "implementing" GenericSourceType, see e.g. ModuleDependencies
// \todo may be this will not be flexible enough, consider some other variants

public fun <T : Rule, S: GenericSourceType> T.addToSources(sources: RuleSources, vararg args: S): T = addToSources(sources, args.asIterable())

public fun <T : Rule, S: GenericSourceType> T.addToSources(sources: RuleSources, args: Iterable<S>): T {
    for (arg in args)
        when (arg) {
            is ArtifactsSet -> sources.artifacts.addAll(arg.members)
            is Array<out GenericSourceType> -> addToSources(sources, arg.asIterable())
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

public fun <T : Rule, S: GenericSourceType> T.from(args: Iterable<S>): T = addToSources(explicitFroms, args)
public fun <T : Rule, S: GenericSourceType> T.from(vararg args: S): T = addToSources(explicitFroms, *args)

public fun <T : Rule, S: GenericSourceType> T.dependsOn(args: Iterable<S>): T = addToSources(explicitDepends, args)
public fun <T : Rule, S: GenericSourceType> T.dependsOn(vararg args: S): T = addToSources(explicitDepends, *args)

public fun <T : Rule> T.into(vararg artifacts: Iterable<Artifact>): T {
    artifacts.forEach { explicitTargets.addAll(it) }
    return this
}

public fun <T : Rule> T.into(vararg artifacts: Artifact): T {
    explicitTargets.addAll(artifacts)
    return this
}

// \todo: find better way to export
public fun <T : Rule> T.export(vararg artifacts: Iterable<Artifact>): T {
    export = true
    return into(*artifacts)
}

public fun <T : Rule> T.export(vararg artifacts: Artifact): T {
    export = true
    return into(*artifacts)
}


public class LambdaRule(
       public var body: ((Iterable<Pair<ArtifactDesc, ArtifactData?>>, Iterable<ArtifactDesc>) -> Iterable<ArtifactData>)? = null
) : RuleImpl(), LambdaStep {

    companion  object {
        internal var counter = 0;
    }
    override var name: String = "lambda${++counter}"

    override val func: (Iterable<Pair<ArtifactDesc, ArtifactData?>>, Iterable<ArtifactDesc>) -> Iterable<ArtifactData>
        get() = body!!
}


public fun LambdaRule.invoke(f: (Iterable<Pair<ArtifactDesc, ArtifactData?>>, Iterable<ArtifactDesc>) -> Iterable<ArtifactData>): LambdaRule {
    body = f
    return this
}


public open class BasicToolRule<Config, T: Tool<Config>>(
        override val tool: T
) : RuleImpl(), ToolStep<Config, T> {
    override val config: Config = this as Config

    override var name: String = tool.name
    override val sources: Iterable<ArtifactDesc> get() = super<RuleImpl>.sources
}


public val tools.custom: LambdaRule get() = LambdaRule()
public fun tools.custom( body: ((Iterable<Pair<ArtifactDesc, ArtifactData?>>, Iterable<ArtifactDesc>) -> Iterable<ArtifactData>)): LambdaRule = LambdaRule(body)

