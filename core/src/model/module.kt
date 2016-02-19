
package komplex.model

import komplex.utils.Named

interface ModuleCollection {
    val parent: Module?
    val children: Iterable<Module>
}


interface ModuleMetadata {}


interface Module : ModuleCollection, Named {
    val metadata: ModuleMetadata
    val dependencies: Iterable<ConditionalModuleDependency>
    val steps: Iterable<Step>
    val defaultScenario: Scenarios get() = Scenarios.All
    val fullName: String get() = generateSequence(this, { it.parent }).toList().reversed().map { it.name }.joinToString(".")

    fun ownSources(scenarios: Scenarios): Iterable<ArtifactDesc> =
            steps.filter { scenarios.matches(it.selector) }.flatMap { it.sources }

    fun sources(scenarios: Scenarios): Iterable<ArtifactDesc> =
        children.flatMap { it.sources(scenarios) } +
        dependencies
                .filter { scenarios.matches(it.selector) }
                .flatMap { modDep -> modDep.module.targets(
                        when (modDep.scenarios) {
                            Scenarios.Default_ -> Scenarios.Default_
                            Scenarios.Same -> scenarios
                            else -> modDep.scenarios
                })} +
        ownSources(scenarios)

    fun targets(scenarios: Scenarios = Scenarios.Default_): Iterable<ArtifactDesc> =
        children.flatMap { it.targets( if (scenarios == Scenarios.Default_) defaultScenario else scenarios) } +
        steps.filter { scenarios.matches(it.selector) && it.export }.flatMap {
            it.targets }
}


