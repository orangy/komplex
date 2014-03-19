package komplex

data class ProjectDependency(val config: Config, val reference: ProjectReference)
data class LibraryDependency(val config: Config, val reference: LibraryReference)


fun Dependencies() = Dependencies(Config("*"), arrayListOf(), arrayListOf())

class Dependencies(val config: Config,
                   val projects: MutableList<ProjectDependency>,
                   val libraries: MutableList<LibraryDependency>) {

    fun invoke(config: Config): Dependencies {
        return Dependencies(config, projects, libraries)
    }

    fun on(dependencies: ProjectReferences) = projects.addAll(dependencies.map { ProjectDependency(config, it) })
    fun on(dependencies: LibraryReferences) = libraries.addAll(dependencies.map { LibraryDependency(config, it) })

    fun on(reference: ProjectReference) = on(ProjectReferences(reference))
    fun on(library: LibraryReference) = on(LibraryReferences(library))
    fun on(project: Project) = on(ProjectReferences(project.projectName))

    fun dump(indent: String = "") {
        if (projects.size == 0 && libraries.size == 0)
            return

        println("$indent Depends on")
        for ((config, reference) in projects) {
            println("$indent   Project: ${reference.name} (in ${config.pattern})")
        }
        for ((config, reference) in libraries) {
            println("$indent   Library: ${reference.name} (in ${config.pattern})")
        }
    }

}
