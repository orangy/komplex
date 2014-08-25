package komplex

data class ModuleDependency(val config: Config, val reference: ModuleReference)


fun Dependencies() = Dependencies(Config("*"), arrayListOf())

class Dependencies(val config: Config, val modules: MutableList<ModuleDependency>) {

    public fun invoke(config: Config): Dependencies {
        return Dependencies(config, modules)
    }

    public fun on(dependencies: ModuleReferences): Unit {
        modules.addAll(dependencies.map { ModuleDependency(config, it) })
    }

    public fun on(reference: ModuleReference): Unit = on(ModuleReferences(reference))
    public fun on(project: Module): Unit = on(ModuleReferences(project.moduleName))

    fun dump(indent: String = "") {
        if (modules.size == 0)
            return

        println("$indent Depends on")
        for ((config, reference) in modules) {
            println("$indent   Project: ${reference.name} (in ${config.pattern})")
        }
    }

}
