package komplex

/**
 * Provides DSL for building hierarchy of modules
 */
public open class ModuleCollection() {
    val modules = arrayListOf<Module>()

    public fun invoke(name: String, description: String? = null, body: Module.() -> Unit): Module = module(name, description, body)

    public fun module(name: String, description: String? = null, body: Module.() -> Unit): Module {
        val project = Module(name)
        if (description != null)
            project.description(description)
        project.body()
        modules.add(project)
        return project
    }

    public open fun dump(indent: String) {
        for (child in modules) {
            println()
            println("$indent Module: ${child.title}")
            child.dump(indent + "  ")
        }
    }
}