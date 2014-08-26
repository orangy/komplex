package komplex

/**
 * Provides DSL for building hierarchy of modules
 */
public open class ModuleCollection(val parent: Module) {
    val modules = arrayListOf<Module>()

    public fun invoke(name: String, description: String? = null, body: Module.() -> Unit): Module {
        val project = Module(name, parent)
        if (description != null)
            project.description(description)
        project.body()
        modules.add(project)
        return project
    }
}