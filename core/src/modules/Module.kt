package komplex

import komplex.Event
import komplex.ModuleScript
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
    public val build: ModuleScript = ModuleScript(this)

    public val module: ModuleCollection = ModuleCollection(this)

    public fun dump(indent: String) {
        println("$indent Version: $version")
        depends.dump(indent)
        build.dump(indent)

        for (child in module.modules) {
            println()
            println("$indent Module: ${child.title}")
            child.dump(indent + "  ")
        }
    }
}

