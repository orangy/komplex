package komplex

import java.util.ArrayList

open class Project(val projectName: String, val parent: Project?) : ProjectConfiguration() {
    override val title: String
        get() = if (description.isEmpty()) projectName else "$projectName ($description)"

    val projects = arrayListOf<Project>()
    val shared = arrayListOf<SharedConfiguration>()

    fun shared(pattern: String = "*", body: SharedConfiguration.() -> Unit): SharedConfiguration {
        val setting = SharedConfiguration(pattern, this)
        setting.body()
        shared.add(setting)
        return setting
    }

    fun project(name: String): ProjectReference = ProjectReference(name)

    fun project(name: String, body: Project.() -> Unit): Project {
        val project = Project(name, this)
        project.body()
        projects.add(project)
        return project
    }

    override fun dump(block: Block, indent : String) {
        super.dump(block, indent)
        for (child in projects) {
            println("$indent Project: ${child.title}")
            child.dump(block, indent + "  ")
        }
        for (child in shared) {
            println("$indent Shared: ${child.title}")
            child.dump(block, indent + "  ")
        }
    }
}

