package komplex

/**
 *
 */
public open class Module(parent1: Module?, public val moduleName: String) : ModuleCollection(parent1) {
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

    public fun targets(scenario: Scenario): Iterable<Artifact> = // \todo add caching
        build.ruleSets.filter { it.selectors.any { it.matches(scenario) }}
                      .flatMap { it.rules }
                      //.filter { it.export } // \todo find a way to filter all exports as in graph
                      .flatMap { it.targets(scenario) }

    override fun toString(): String = "$title"
}

