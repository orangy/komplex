package komplex

import komplex.Event
import komplex.ModuleScenarios
import komplex.SharedSettings
import komplex.Dependencies
import komplex.ModuleReference
import komplex.EventStyle

/**
 *
 */
public open class Module(public val moduleName: String, public val parent: Module?) {
    public val title: String
        get() = if (description.isEmpty()) moduleName else "$moduleName ($description)"

    private var _version: String = ""
    public val version: String
        get() = _version
    public fun version(value: String) {
        _version = value
    }

    private var _description: String = ""
    public val description: String
        get() = _description
    fun description(value: String) {
        _description = value
    }

    public val depends: Dependencies = Dependencies()
    public val build: ModuleScenarios = ModuleScenarios(this)

    public val module: Modules = Modules(this)

    public fun dump(indent: String) {
        println("$indent Version: $version")
        depends.dump(indent)
        build.dump(indent)

        for (child in module.modules) {
            println("$indent Project: ${child.title}")
            child.dump(indent + "  ")
        }
    }
}

