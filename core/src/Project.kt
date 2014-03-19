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
    val build = Builds()

    val building = Event("building")
    val built = Event("built", EventStyle.reversed)

    fun shared(pattern: String = "*", body: Project.() -> Unit) {
        val setting = SharedSettings(pattern, this, body)
        sharedSettings.add(setting)
    }

    fun applySharedSettings(settings: List<SharedSettings>) {
        for (config in settings) {
            if (config.matches(this)) {
                val initializer = config.body
                initializer()
            }
        }

        val nestedSettings = settings + sharedSettings
        for (project in projects)
            project.applySharedSettings(nestedSettings)
    }

    fun project(name: String): ProjectReference = ProjectReference(name)

    fun project(name: String, description : String? = null, body: Project.() -> Unit): Project {
        val project = Project(name, this)
        if (description != null)
            project.description(description)
        project.body()
        projects.add(project)
        return project
    }

    fun dump(indent: String) {
        println("$indent Version: $version")
        depends.dump(indent)
        build.dump(indent)

        for (child in projects) {
            println("$indent Project: ${child.title}")
            child.dump(indent + "  ")
        }
    }
}

