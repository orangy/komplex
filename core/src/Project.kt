package komplex

import java.util.*

public fun project(body: Project.() -> Unit): Project {
    val project = Project()
    project.body()
    return project
}

public class Project() : Module("<block>", null) {

    val moduleReferences = HashMap<String, Module>()
    val modulesBuilt = HashMap<Module, BuildResult>()

    fun build(config: String = "") {
        applySharedSettings(listOf())
        //dump("")

        makeProjectSet(module.modules)

        println("========= BUILD =========")
        for (project in module.modules)
            build(project, config)
    }

    fun makeProjectSet(projects: List<Module>) {
        for (project in projects) {
            moduleReferences.put(project.moduleName, project)
            makeProjectSet(project.module.modules)
        }
    }

    fun resolve(reference: ModuleReference): Module? {
        return moduleReferences[reference.name]
    }

    fun build(project: Module, config: String): BuildResult {
        val existingResult = modulesBuilt[project]
        if (existingResult != null)
            return existingResult

        // build dependencies
        for (dependency in project.depends.modules) {
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
        for (nestedProject in project.module.modules) {
            val result = build(nestedProject, config)
            if (result.failed) return result
        }

        project.building.fire(project)

        val projectResult = ModuleBuildResult()

        // now execute own build processes
        for (buildConfig in project.build.configurations) {
            if (buildConfig.configurations.any { it.matches(config) }) {
                for (step in buildConfig.steps) {
                    val result = step.execute(BuildContext(config, project, step))
                    projectResult.append(result)
                    if (projectResult.failed) break;
                }
                if (projectResult.failed) break;
            }
        }

        modulesBuilt.put(project, projectResult)
        project.built.fire(project)

        return projectResult
    }
}