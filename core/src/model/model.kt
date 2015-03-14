
package komplex.model

import komplex.utils.BuildDiagnostic
import komplex.utils.Named


public trait ArtifactDesc : Named {}

public trait ArtifactData {}

public trait Scenario {}

public open class Scenarios(public val items: Collection<Scenario>) {
    public fun combine(other: Scenarios): Scenarios = when {
        this == All || other == None || other == Same || other == Default_ -> this
        this == None || this == Same || this == Default_ || other == All -> other
        else -> Scenarios((items + other.items).distinct())
    }
    class object {
        val All = Scenarios(listOf<Scenario>())
        val Same = Scenarios(listOf<Scenario>())
        val Default_ = Scenarios(listOf<Scenario>())
        val None = Scenarios(listOf<Scenario>())
    }
}

public data class ScenarioSelector(public val scenarios: Scenarios) {
    class object {
        val Any = ScenarioSelector(Scenarios.All)
        val None = ScenarioSelector(Scenarios.None)
    }
}

public fun<T: Scenarios> T.matches(selector: ScenarioSelector): Boolean =
    when {
        this == Scenarios.None -> false
        selector == ScenarioSelector.Any -> true
        selector == ScenarioSelector.None -> false
        this == Scenarios.All -> true
        this == Scenarios.Same || this == Scenarios.Default_ -> throw Exception("Unresolved scenario: $this")
        else -> this.items.any { selector.scenarios.items.contains(it) }
    }

public fun ScenarioSelector.combine(selector: ScenarioSelector): ScenarioSelector =
   when {
       this == ScenarioSelector.Any || selector == ScenarioSelector.None -> this
       this == ScenarioSelector.None || selector == ScenarioSelector.Any -> selector
       else -> ScenarioSelector((this.scenarios.combine(selector.scenarios)))
   }

public fun<T: ScenarioSelector> Iterable<T>.combine(defaultToAny: Boolean = false): ScenarioSelector =
    if (this.none()) (if (defaultToAny) ScenarioSelector.Any else ScenarioSelector.None)
    else this.reduce { (a: ScenarioSelector, b: ScenarioSelector) -> a.combine(b) }

public fun Scenarios.resolve(current: Scenarios, default: Scenarios = Scenarios.All): Scenarios =
    when {
        this == Scenarios.All -> this
        this == Scenarios.Same -> current
        this == Scenarios.Default_ -> default
        else -> this
    }


public trait BuildContext{
    public val scenario: Scenarios
    public val module: Module
}

public data class BuildResult(public val diagnostic: BuildDiagnostic,
                              public val result: Iterable<Pair<ArtifactDesc, ArtifactData?>> = listOf())
//                              public val dependsOn: Iterable<Pair<ArtifactDesc, ArtifactData?>> = listOf())
{}


// build step, tied to single scenario in order to build fixed relationships in graph
public trait Step : Named {
    public val selector: ScenarioSelector // determines if the step should be selected for execution for given scenario(s)
    public val sources: Iterable<ArtifactDesc> // consumed artifacts
    public val export: Boolean // defines if step targets are local or exported from the module
    public val targets: Iterable<ArtifactDesc> // produced artifacts

    public fun execute(context: BuildContext, artifacts: Map<ArtifactDesc, ArtifactData?> = hashMapOf()) : BuildResult
            = BuildResult(BuildDiagnostic.Success)
}


public trait ModuleDependency {
    public val module: Module
    public val selector: ScenarioSelector
    public val scenarios: Scenarios // referenced module build scenario
}

public fun ModuleDependency.sources(srcScenarios: Scenarios): Iterable<ArtifactDesc> =
    if (srcScenarios.matches(selector))
        module.sources(scenarios.resolve(srcScenarios))
    else listOf()

public fun ModuleDependency.targets(tgtScenarios: Scenarios): Iterable<ArtifactDesc> =
    if (tgtScenarios.matches(selector))
        module.targets(scenarios.resolve(tgtScenarios))
    else listOf()

/*
public fun <A> extractArtifacts(vararg args: A): Iterable<ArtifactDesc> =
    when (args.first()) {
        is ArtifactDesc -> args.map { it as ArtifactDesc }
        // now ignoring the selector
        // \todo "lift" selectors to the result (return artifacts grouped by selector)
        is ExplicitDependency<*> -> args.map { (it as ExplicitDependency<ArtifactDesc>).reference }
        else -> throw IllegalArgumentException("Cannot extract Artifacts from $args")
    }
*/
/*
public fun <A> (vararg artifacts: Iterable<A>): T {
    val sample = artifacts.firstOrNull { it.any() }?.first()
    when {
        sample is Artifact -> artifacts.forEach { explicitSources.addAll(it as Iterable<Artifact>) }
    // now ignoring the selector
    // \todo "lift" the selector to the rule somehow
        sample is ExplicitDependency<*> ->
            artifacts.forEach { it.map { (it as ExplicitDependency<Artifact>).reference }.forEach { explicitSources.add(it) } }
    }
    return this
}
*/
