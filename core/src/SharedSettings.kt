package komplex

class SharedSettings(val pattern: String,
                          val parent: Project,
                          val body: Project.() -> Unit)  {

    val regex = pattern.replace("?",".").replace("*", ".*")
    fun matches(project: Project) : Boolean = project.projectName.matches(regex)
}

