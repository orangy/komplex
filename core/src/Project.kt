package komplex

open class Project(val projectName: String, val parent: Project?) {
    val title: String
        get() = if (description.isEmpty()) projectName else "$projectName ($description)"

    val projects = arrayListOf<Project>()
    val sharedSettings = arrayListOf<SharedSettings>()

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
    val dumping = Event("dumping")

    fun shared(pattern: String = "*", body: Project.() -> Unit) {
        val setting = SharedSettings(pattern, this, body)
        sharedSettings.add(setting)
    }

    fun applySharedSettings(configurations: List<SharedSettings>) {
        for (config in configurations) {
            if (config.matches(this)) {
                val initializer = config.body
                initializer()
            }
        }
        val nestedConfigurations = configurations + sharedSettings
        for (project in projects)
            project.applySharedSettings(nestedConfigurations)
    }

    fun project(name: String): ProjectReference = ProjectReference(name)

    fun project(name: String, body: Project.() -> Unit): Project {
        val project = Project(name, this)
        project.body()
        projects.add(project)
        return project
    }

    fun dump(indent: String) {
        dumping.fire(this)
        println("$indent Version: $version")
        depends.dump(indent)
        build.dump(indent)

        for (child in projects) {
            println("$indent Project: ${child.title}")
            child.dump(indent + "  ")
        }
    }
}

