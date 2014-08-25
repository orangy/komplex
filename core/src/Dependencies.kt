package komplex

public data class ModuleDependency(val scenario: Scenario, val reference: ModuleReference)

public fun Dependencies(): Dependencies = Dependencies(Scenario("*"), arrayListOf())

public class Dependencies(val config: Scenario, val dependencies: MutableList<ModuleDependency>) {

    public fun invoke(scenario: Scenario): Dependencies {
        return Dependencies(scenario, dependencies)
    }

    public fun on(references: ModuleReferences): Unit {
        dependencies.addAll(references.map { ModuleDependency(config, it) })
    }

    public fun on(reference: ModuleReference): Unit = on(ModuleReferences(reference))
    public fun on(module: Module): Unit = on(ModuleReferences(module.moduleName))

    fun dump(indent: String = "") {
        if (dependencies.size == 0)
            return

        println("$indent Depends on")
        for ((scenario, reference) in dependencies) {
            println("$indent   Project: ${reference} (in ${scenario})")
        }
    }

}
