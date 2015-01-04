package komplex

/**
 * Collection of modules
 */
public open class ModuleCollection(public val parent: Module? = null) {
    public val modules: MutableList<Module> = arrayListOf()

    public fun module(name: String, description: String? = null, body: Module.() -> Unit): Module {
        val module = Module(parent, name)
        if (description != null)
            module.description(description)
        module.body()
        modules.add(module)
        return module
    }
}