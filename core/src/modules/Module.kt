package komplex

/**
 *
 */
public open class Module(public val moduleName: String) : ModuleCollection() {
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
    public fun description(value: String) {
        _description = value
    }

    public val depends: Dependencies = Dependencies()
    public val build: ModuleScript = ModuleScript(this)


    override fun toString(): String = "$title"
}

