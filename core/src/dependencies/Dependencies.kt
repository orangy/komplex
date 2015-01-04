package komplex

public open class DependencyGroup(val selectors: List<ScenarioSelector>) {
    val moduleDeps: MutableList<Module> = arrayListOf()
    val artifactDeps: MutableList<Artifact> = arrayListOf()

    public class References() {
        val modules: MutableList<Module> = arrayListOf()
        val artifacts: MutableList<Artifact> = arrayListOf()

        public fun module(module: Module): Unit { modules.add(module) }
        public fun library(name: String, version: String? = null): Unit { artifacts.add(LibraryReferenceArtifact(name, version)) }
    }

    public fun on(vararg ms: Module): Unit { moduleDeps.addAll(ms) }
    public fun on(body: References.() -> Unit) {
        var references = References()
        references.body()
        moduleDeps.addAll(references.modules)
        artifactDeps.addAll(references.artifacts)
    }
}


public class Dependencies(vararg scenarios: ScenarioSelector = array(ScenarioSelector("*"))) : DependencyGroup(scenarios.toList()) {
    val groups: MutableList<DependencyGroup> = arrayListOf(this)

    inline public fun invoke(vararg scenarios: ScenarioSelector, body: DependencyGroup.() -> Unit): DependencyGroup {
        val group = invoke(*scenarios)
        group.body()
        return group
    }

    public fun invoke(vararg scenarios: ScenarioSelector): DependencyGroup {
        val group = DependencyGroup(scenarios.toList())
        groups.add(group)
        return group
    }

    public fun getModulesCollector(): (scenario: Scenario) -> Iterable<Module> {
        return {(scenario) -> groups.filter { it.selectors.any { it.matches(scenario) } }.flatMap { it.moduleDeps } }
    }

    public fun getArtifactsCollector(): (scenario: Scenario) -> Iterable<Artifact> {
        return { (scenario) ->
            groups.filter {
                it.selectors.any { it.matches(scenario) }
            }.flatMap { it.artifactDeps }
        }
    }


    public val modules: (scenario: Scenario) -> Iterable<Module> by object {
        fun get(dependencies: Dependencies, propertyMetadata: PropertyMetadata): Function1<Scenario, Iterable<Module>> =
                dependencies.getModulesCollector()
    }

    public val artifacts: (scenario: Scenario) -> Iterable<Artifact> by object {
        fun get(dependencies: Dependencies, propertyMetadata: PropertyMetadata): Function1<Scenario, Iterable<Artifact>> =
                dependencies.getArtifactsCollector()
    }
}
