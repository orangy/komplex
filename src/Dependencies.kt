package komplex

data class ProjectDependency(val config: Configuration?, val reference: ProjectReference)
data class LibraryDependency(val config: Configuration?, val reference: LibraryReference)


fun Dependencies() = Dependencies(null, arrayListOf(), arrayListOf())

class Dependencies(val config: Configuration? = null,
                   val projects: MutableList<ProjectDependency>,
                   val libraries: MutableList<LibraryDependency>) {

    fun invoke(config: Configuration): Dependencies {
        return Dependencies(config, projects, libraries)
    }

    fun on(dependencies: ProjectReferences) = projects.addAll(dependencies.map { ProjectDependency(config, it) })
    fun on(dependencies: LibraryReferences) = libraries.addAll(dependencies.map { LibraryDependency(config, it) })

    fun on(reference: ProjectReference) = on(ProjectReferences(reference))
    fun on(library: LibraryReference) = on(LibraryReferences(library))
    fun on(project: Project) = on(ProjectReferences(project.projectName))

    fun dump(block : Block, indent: String = "") {
        if (projects.size == 0 && libraries.size == 0)
            return

        println("$indent Depends on")
        for ((config, reference) in projects) {
            if (config != null)
                println("$indent   Project: ${reference.name} (in ${config.name})")
            else
                println("$indent   Project: ${reference.name}")
        }
        for ((config, reference) in libraries) {
            if (config != null)
                println("$indent   Library: ${reference.name} (in ${config.name})")
            else
                println("$indent   Library: ${reference.name}")
        }
    }

}
