package komplex

abstract class ProjectConfiguration {
    abstract val title: String

    private var _version: String = ""
    public val version: String
        get() = _version
    fun version(value: String) {
        _version = value
    }

    private var _description: String = ""
    public val description: String
        get() = _description
    fun description(value: String) {
        _description = value
    }

    val depends = Dependencies()
    val build = Build()

    val building = Event("building")
    val built = Event("built", EventStyle.reversed)

    open fun dump(block: Block, indent : String = "") {
        println("$indent Version: $version")
        depends.dump(block, indent)
        build.dump(block, indent)
    }
}
