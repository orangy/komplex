
package komplex.model

import komplex.utils.Named

public interface ModuleCollection {
    public val parent: Module?
    public val children: Iterable<Module>
}


public interface ModuleMetadata {}


public interface Module : ModuleCollection, Named {
    public val metadata: ModuleMetadata
    public val dependencies: Iterable<ConditionalModuleDependency>
    public val steps: Iterable<Step>
    public val defaultScenario: Scenarios get() = Scenarios.All
    public val fullName: String get() = sequence(this, { it.parent }).toList().reverse().map { it.name }.joinToString(".")

    public fun ownSources(scenarios: Scenarios): Iterable<ArtifactDesc> =
            steps.filter { scenarios.matches(it.selector) }.flatMap { it.sources }

    public fun sources(scenarios: Scenarios): Iterable<ArtifactDesc> =
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

    public fun targets(scenarios: Scenarios = Scenarios.Default_): Iterable<ArtifactDesc> =
        children.flatMap { it.targets( if (scenarios == Scenarios.Default_) defaultScenario else scenarios) } +
        steps.filter { scenarios.matches(it.selector) && it.export }.flatMap {
            it.targets }
}


