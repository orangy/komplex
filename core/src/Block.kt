package komplex

import java.util.*

public fun block(body: Block.() -> Unit): Block {
    val block = Block()
    block.body()
    return block
}


public class Block() : Project("<block>", null) {

    val projectReferences = HashMap<String, Project>()
    val projectBuilt = HashMap<Project, BuildResult>()

    fun build(config: String = "") {
        applySharedSettings(listOf())
        //dump("")

        makeProjectSet(projects)

        println("========= BUILD =========")
        for (project in projects)
            build(project, config)
    }

    fun makeProjectSet(projects: List<Project>) {
        for (project in projects) {
            projectReferences.put(project.projectName, project)
            makeProjectSet(project.projects)
        }
    }

    fun resolve(reference: ProjectReference): Project? {
        return projectReferences[reference.name]
    }

    fun build(project: Project, config: String): BuildResult {
        val existingResult = projectBuilt[project]
        if (existingResult != null)
            return existingResult

        // build dependencies
        for (dependency in project.depends.projects) {
            if (dependency.config.matches(config)) {
                val dependentProject = resolve(dependency.reference)
                if (dependentProject == null) {
                    println("Invalid project reference ${dependency.reference}")
                } else {
                    val result = build(dependentProject, config)
                    if (result.failed) return result
                }
            }
        }

        // build nested projects
        for (nestedProject in project.projects) {
            val result = build(nestedProject, config)
            if (result.failed) return result
        }

        project.building.fire(project)

        val projectResult = MultipleBuildResult()

        // now execute own build processes
        for (buildConfig in project.build.configurations) {
            if (buildConfig.configurations.any { it.matches(config) }) {
                for (process in buildConfig.processes) {
                    val result = process.execute(BuildContext(config, project, process))
                    projectResult.append(result)
                    if (projectResult.failed) break;
                }
                if (projectResult.failed) break;
            }
        }

        projectBuilt.put(project, projectResult)
        project.built.fire(project)

        return projectResult
    }
}