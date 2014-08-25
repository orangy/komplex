package komplex

public open class ModuleBuilder(val parent: Module) {
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

public trait ModuleSource {

}

public fun maven(name: String, version: String = "", pkg: String = ""): ModuleSource {
    return object : ModuleSource {}
}

public fun ModuleBuilder.from(source: ModuleSource): Module {
    return Module("sdfsdf", parent)
}

public open class Module(public val moduleName: String, public val parent: Module?) {
    public val title: String
        get() = if (description.isEmpty()) moduleName else "$moduleName ($description)"

    val sharedSettings = arrayListOf<SharedSettings>()

    public val repository: Repository = Repository(this)

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
    public val build: BuildModule = BuildModule(this)

    public val building: Event<Module> = Event<Module>("building")
    public val built: Event<Module> = Event<Module>("built", EventStyle.reversed)

    public fun shared(pattern: String = "*", body: Module.() -> Unit) {
        val setting = SharedSettings(pattern, this, body)
        sharedSettings.add(setting)
    }

    public fun repository(body: Repository.() -> Unit) {
        repository.body()
    }

    fun applySharedSettings(settings: List<SharedSettings>) {
        for (config in settings) {
            if (config.matches(this)) {
                val initializer = config.body
                initializer()
            }
        }

        val nestedSettings = settings + sharedSettings
        for (project in module.modules)
            project.applySharedSettings(nestedSettings)
    }

    public fun module(name: String): ModuleReference = ModuleReference(name)

    public val module: ModuleBuilder = ModuleBuilder(this)


    fun dump(indent: String) {
        println("$indent Version: $version")
        depends.dump(indent)
        build.dump(indent)

        for (child in module.modules) {
            println("$indent Project: ${child.title}")
            child.dump(indent + "  ")
        }

        repository.dump(indent + "  ")
    }
}

