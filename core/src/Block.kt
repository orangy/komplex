package komplex

import java.util.HashMap

public fun block(body: Block.() -> Unit): Block {
    val block = Block()
    block.body()
    return block
}


public class Block() : Project("<block>", null) {

    val projectSet = HashMap<String, Project>()
    fun build(config: String = "") {
        applySharedSettings(listOf())
        dump("")

        makeProjectSet(projects)

        println("========= BUILD =========")
        for (project in projects)
            build(project, config)
    }

    fun makeProjectSet(projects: List<Project>) {
        for (project in projects) {
            projectSet.put(project.projectName, project)
            makeProjectSet(project.projects)
        }
    }

    fun resolve(reference: ProjectReference): Project? {
        return projectSet[reference.name]
    }

    fun build(project: Project, config: String) {
        project.building.fire(project)

        // first build nested projects
        for (nestedProject in project.projects) {
            build(nestedProject, config)
        }

        // then build dependencies
        for (dependency in project.depends.projects) {
            if (dependency.config.matches(config)) {
                val dependentProject = resolve(dependency.reference)
                if (dependentProject == null) {
                    println("Invalid project reference ${dependency.reference}")
                } else {
                    build(dependentProject, config)
                }
            }
        }

        // now execute own build processes
        for (step in project.build.steps) {
            if (step.configurations.any { it.matches(config) }) {
                for (process in step.processes) {
                    process.execute()
                }
            }
        }

        project.built.fire(project)
    }
}