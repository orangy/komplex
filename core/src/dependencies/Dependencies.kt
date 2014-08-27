package komplex

public data class ModuleDependency(val scenario: ScenarioSelector, val reference: ModuleReference)

public fun Dependencies(): Dependencies = Dependencies(ScenarioSelector("*"), arrayListOf())

public class Dependencies(val scenario: ScenarioSelector, val dependencies: MutableList<ModuleDependency>) {

    public fun invoke(scenario: ScenarioSelector): Dependencies {
        return Dependencies(scenario, dependencies)
    }

    public fun on(references: ModuleReferences): Unit {
        dependencies.addAll(references.map { ModuleDependency(scenario, it) })
    }

    public fun on(reference: ModuleReference): Unit = on(ModuleReferences(reference))
    public fun on(module: Module): Unit = on(ModuleReferences(module.moduleName))
}
