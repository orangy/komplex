package komplex

/**
 * Collection of modules
 */
public open class ModuleCollection() {
    val modules = arrayListOf<Module>()

    public fun module(name: String, description: String? = null, body: Module.() -> Unit): Module {
        val project = Module(name)
        if (description != null)
            project.description(description)
        project.body()
        modules.add(project)
        return project
    }
}