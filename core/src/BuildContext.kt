package komplex

public data class BuildContext(val config: String,
                          val project: Project,
                          val process: BuildProcess) {

    fun dependencies() {
        val projectDependencies = project.depends.projects.filter { it.config.matches(config) }.map { it.reference }
        val libraryDependencies = project.depends.libraries.filter { it.config.matches(config) }.map { it.reference }

    }
}