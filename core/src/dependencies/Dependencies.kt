package komplex

public data class ModuleDependency(val scenario: ScenarioSelector, val reference: Reference)

public fun Dependencies(): Dependencies = Dependencies(ScenarioSelector("*"), arrayListOf())

public class Dependencies(val scenario: ScenarioSelector, val dependencies: MutableList<ModuleDependency>) {

    public fun invoke(scenario: ScenarioSelector): Dependencies {
        return Dependencies(scenario, dependencies)
    }

    public fun on(references: References): Unit {
        dependencies.addAll(references.map { ModuleDependency(scenario, it) })
    }

    public fun on(reference: Reference): Unit = on(References(reference))
    public fun on(module: Module): Unit = on(References(ModuleReference(module)))
    public fun on(body: References.() -> Unit) {
        var references = References()
        references.body()
        on(references)
    }
}
