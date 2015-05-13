
package komplex.dsl

import komplex.model.*
import java.util.ArrayList
import komplex.utils.BuildDiagnostic

// rules in fact
public object tools {}

// \todo optimize with separating Step and Rule, so rule could be mutable, but converted into immutable Step then added to the graph,
// or alternatively make rule immutable with copy-on-write behaviour

public data class RuleSources {
    public val artifacts: MutableCollection<ArtifactDesc> = arrayListOf()
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

public trait Rule : Step {
    public val explicitFroms: RuleSources
    public val explicitDepends: RuleSources
    public val explicitTargets: MutableCollection<ArtifactDesc> // filled with "into" and "export"

    public val fromSources: Iterable<ArtifactDesc> get() = explicitFroms.collect(selector.scenarios)
    public val dependsSources: Iterable<ArtifactDesc> get() = explicitDepends.collect(selector.scenarios)

    override var selector: ScenarioSelector

    override val sources: Iterable<ArtifactDesc> get() = fromSources + dependsSources
    override var export: Boolean
    override val targets: Iterable<ArtifactDesc> get() = explicitTargets
}


public abstract class RuleImpl : Rule {
    override var selector: ScenarioSelector = ScenarioSelector.Any
    override var export: Boolean = false
    override val explicitFroms: RuleSources = RuleSources()
    override val explicitDepends: RuleSources = RuleSources()
    override val explicitTargets: MutableCollection<ArtifactDesc> = arrayListOf()
}

public fun <TR : Rule> TR.with(body: TR.() -> Unit): TR {
    body()
    return this
}

public fun <T : Rule> T.addToSources(sources: RuleSources, vararg args: Any): T = addToSources(sources, args.asIterable())

public fun <T : Rule> T.addToSources(sources: RuleSources, args: Iterable<Any>): T {
    for (arg in args)
        when (arg) {
            is ArtifactsSet -> sources.artifacts.addAll(arg.members)
            is Iterable<*> -> addToSources(sources, arg as Iterable<Any>)
            is ArtifactDesc -> sources.artifacts.add(arg)
            is ModuleDependency -> sources.moduleDependencies.add(arg)
            is Module -> sources.modules.add(arg)
            is Rule -> sources.rules.add(arg)
            else -> throw Exception("Unknown source type: $arg")
        }
    return this
}

public fun <T : Rule, S> T.from(args: Iterable<S>): T = addToSources(explicitFroms, args)
public fun <T : Rule, S> T.from(vararg args: S): T = addToSources(explicitFroms, args.asIterable())
public fun <T : Rule, S> T.from(vararg args: Iterable<S>): T = addToSources(explicitFroms, args.asIterable())

public fun <T : Rule, S> T.depends(args: Iterable<S>): T = addToSources(explicitDepends, args)
public fun <T : Rule, S> T.depends(vararg args: S): T = addToSources(explicitDepends, args.asIterable())
public fun <T : Rule, S> T.depends(vararg args: Iterable<S>): T = addToSources(explicitDepends, args.asIterable())

public fun <T : Rule> T.into(vararg artifacts: Iterable<Artifact>): T {
    artifacts.forEach { explicitTargets.addAll(it) }
    return this
}

public fun <T : Rule> T.into(vararg artifacts: Artifact): T {
    explicitTargets.addAll(artifacts)
    return this
}

// \todo: find better way to export
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
}


public val tools.custom: LambdaRule get() = LambdaRule()
public fun tools.custom( body: ((Iterable<Pair<ArtifactDesc, ArtifactData?>>, Iterable<ArtifactDesc>) -> Iterable<ArtifactData>)): LambdaRule = LambdaRule(body)

