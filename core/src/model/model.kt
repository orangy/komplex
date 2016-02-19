package komplex.model

import komplex.utils.BuildDiagnostic
import komplex.utils.Named
import komplex.utils.plus

interface ArtifactDesc : Named {}

interface ArtifactData {
    val hash: ByteArray
}

interface Scenario {}

open class Scenarios(val items: Iterable<Scenario> = listOf(), override val name: String = "") : Named {
    fun combine(other: Scenarios): Scenarios = when {
        this == All || other == None || other == Same || other == Default_ -> this
        this == None || this == Same || this == Default_ || other == All -> other
        else -> Scenarios((items + other.items).distinct())
    }

    override fun toString(): String = if (name.isEmpty()) items.joinToString(",", "[", "]") else name

    companion object {
        val All = Scenarios(name = "All")
        val Same = Scenarios(name = "Same")
        val Default_ = Scenarios(name = "Default")
        val None = Scenarios(name = "None")
    }
}

data class ScenarioSelector(val scenarios: Scenarios) {
    companion object {
        val Any = ScenarioSelector(Scenarios.All)
        val None = ScenarioSelector(Scenarios.None)
    }
}

fun<T : Scenarios> T.resolved(): Boolean = this != Scenarios.Same && this != Scenarios.Default_

fun<T : Scenarios> T.matches(selector: ScenarioSelector): Boolean =
        when {
            this == Scenarios.None -> false
            selector == ScenarioSelector.Any -> true
            selector == ScenarioSelector.None -> false
            this == Scenarios.All -> true
            !this.resolved() -> throw Exception("Unresolved scenario: $this")
            else -> this.items.any { selector.scenarios.items.contains(it) }
        }

fun ScenarioSelector.combine(selector: ScenarioSelector): ScenarioSelector =
        when {
            this == ScenarioSelector.Any || selector == ScenarioSelector.None -> this
            this == ScenarioSelector.None || selector == ScenarioSelector.Any -> selector
            else -> ScenarioSelector((this.scenarios.combine(selector.scenarios)))
        }

fun Iterable<ScenarioSelector>.combine(defaultToAny: Boolean = false): ScenarioSelector =
        if (this.none()) (if (defaultToAny) ScenarioSelector.Any else ScenarioSelector.None)
        else this.reduce { a: ScenarioSelector, b: ScenarioSelector -> a.combine(b) }

fun Scenarios.resolve(current: Scenarios, default: Scenarios = Scenarios.All): Scenarios =
        when {
            this == Scenarios.All -> this
            this == Scenarios.Same -> current
            this == Scenarios.Default_ -> default
            else -> this
        }


interface BuildContext {
    val scenario: Scenarios
    val module: Module
}

// \todo add used sources to build results, so it would be possible to detect the redundant dependencies
data class BuildResult(val diagnostic: BuildDiagnostic,
                       val result: Iterable<Pair<ArtifactDesc, ArtifactData?>> = listOf())
//                              public val dependsOn: Iterable<Pair<ArtifactDesc, ArtifactData?>> = listOf())
{}

fun BuildResult.plus(other: BuildResult): BuildResult = BuildResult(diagnostic.plus(other.diagnostic), result + other.result)


// build step, tied to single scenario in order to build fixed relationships in graph
interface Step : Named {
    val selector: ScenarioSelector // determines if the step should be selected for execution for given scenario(s)
    val sources: Iterable<ArtifactDesc> // consumed artifacts
    // \todo consider having separate property for exported targets, instead of the flag
    val export: Boolean // defines if step targets are local or exported from the module
    val targets: Iterable<ArtifactDesc> // produced artifacts

    fun execute(context: BuildContext, artifacts: Map<ArtifactDesc, ArtifactData?> = hashMapOf()): BuildResult
            = BuildResult(BuildDiagnostic.Success)
}


interface ConditionalModuleDependency {
    val module: Module
    val selector: ScenarioSelector
    val scenarios: Scenarios // referenced module build scenario
}

fun ConditionalModuleDependency.sources(srcScenarios: Scenarios): Iterable<ArtifactDesc> =
        if (srcScenarios.matches(selector))
            module.sources(scenarios.resolve(srcScenarios))
        else listOf()

fun ConditionalModuleDependency.targets(tgtScenarios: Scenarios): Iterable<ArtifactDesc> =
        if (tgtScenarios.matches(selector))
            module.targets(scenarios.resolve(tgtScenarios))
        else listOf()

