
package komplex.dsl

import java.util.ArrayList
import komplex.model.Step
import komplex.model.ArtifactDesc
import komplex.model.targets
import komplex.model.ScenarioSelector
import komplex.model.LambdaStep
import komplex.model.ArtifactData
import komplex.model.ToolStep
import komplex.model.Tool
import komplex.utils.BuildDiagnostic

// rules in fact
public object tools {}

public trait Rule : Step {
    override var selector: ScenarioSelector
    override val sources: Iterable<ArtifactDesc> get() = explicitSources + depSources
    override var export: Boolean
    override val targets: Iterable<ArtifactDesc> get() = explicitTargets

    public val dependsOn: Iterable<ModuleDependency> get() = explicitDependencies
    val depSources: Iterable<ArtifactDesc> get() = dependsOn.flatMap { it.targets(selector.scenarios) }
    public val explicitDependencies: MutableCollection<ModuleDependency>
    public val explicitSources: MutableCollection<ArtifactDesc>
    public val explicitTargets: MutableCollection<ArtifactDesc>
}


public abstract class RuleImpl : Rule {
    override var selector: ScenarioSelector = ScenarioSelector.Any
    override var export: Boolean = false
    override val explicitDependencies: MutableCollection<ModuleDependency> = arrayListOf()
    override val explicitSources: MutableCollection<ArtifactDesc> = arrayListOf()
    override val explicitTargets: MutableCollection<ArtifactDesc> = arrayListOf()
}

public fun <TR : Rule> TR.with(body: TR.() -> Unit): TR {
    body()
    return this
}

public fun <T : Rule, S> T.from(vararg sources: S): T {
    for (src in sources)
        when (src) {
            is Artifact -> explicitSources.add(src)
            is ModuleDependency -> explicitDependencies.add(src)
        }
    return this
}

public fun <T : Rule, S> T.from(vararg sources: Iterable<S>): T {
    for (src in sources)
        when (src.firstOrNull()) {
            is Artifact -> explicitSources.addAll(src as Iterable<Artifact>)
            is ModuleDependency -> explicitDependencies.addAll(src as Iterable<ModuleDependency>)
        }
    return this
}

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

public class LambdaRule : RuleImpl(), LambdaStep {

    companion  object {
        internal var counter = 0;
    }
    override var name: String = "lambda${++counter}"

    override val func: (Iterable<Pair<ArtifactDesc, ArtifactData?>>, Iterable<ArtifactDesc>) -> Iterable<ArtifactData>
        get() = body!!

    public var body: ((Iterable<Pair<ArtifactDesc, ArtifactData?>>, Iterable<ArtifactDesc>) -> Iterable<ArtifactData>)? = null
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

