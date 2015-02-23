
package komplex.model

import komplex.utils.Named

public trait ModuleCollection {
    public val parent: Module?
    public val children: Iterable<Module>
}


public trait ModuleMetadata {}


public trait Module : ModuleCollection, Named {
    public val metadata: ModuleMetadata
    public val dependencies: Iterable<ModuleDependency>
    public val steps: Iterable<Step>
    public val defaultScenario: Scenarios get() = Scenarios.All

    public fun sources(scenarios: Scenarios): Iterable<ArtifactDesc> =
        children.flatMap { it.sources(scenarios) } +
        dependencies
                .filter { scenarios.matches(it.selector) }
                .flatMap { modDep -> modDep.module.targets(
                        when (modDep.scenarios) {
                            Scenarios.Default -> Scenarios.Default
                            Scenarios.Same -> scenarios
                            else -> modDep.scenarios
                })} +
        steps.filter { scenarios.matches(it.selector) }.flatMap { it.sources }

    public fun targets(scenarios: Scenarios): Iterable<ArtifactDesc> =
        children.flatMap { it.targets( if (scenarios == Scenarios.Default) defaultScenario else scenarios) } +
        steps.filter { scenarios.matches(it.selector) && it.export }.flatMap { it.targets }
}


