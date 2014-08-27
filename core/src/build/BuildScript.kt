package komplex

import java.util.*

public fun script(body: BuildScript.() -> Unit): BuildScript {
    val script = BuildScript()
    script.body()
    return script
}

public class BuildScript() : ModuleCollection() {

    val moduleReferences = HashMap<String, Module>()
    val modulesBuilt = HashMap<Module, BuildResult>()

    fun build(config: String = "") {
        //dump("")

        val scenario = Scenario(config)
        makeModuleSet(modules)

        println("========= BUILD =========")
        for (module in modules)
            build(module, scenario)
    }

    fun makeModuleSet(modules: List<Module>) {
        for (module in modules) {
            moduleReferences.put(module.moduleName, module)
            makeModuleSet(module.modules)
        }
    }

    fun resolve(reference: ModuleReference): Module? {
        return moduleReferences[reference.name]
    }

    fun build(module: Module, scenario: Scenario): BuildResult {
        val existingResult = modulesBuilt[module]
        if (existingResult != null)
            return existingResult

        // build dependencies
        for (dependency in module.depends.dependencies) {
            if (dependency.scenario.matches(scenario)) {
                val dependentModule = resolve(dependency.reference)
                if (dependentModule == null) {
                    println("Invalid module reference ${dependency.reference}")
                } else {
                    val result = build(dependentModule, scenario)
                    if (result.failed) return result
                }
            }
        }

        // build nested modules
        for (nestedModule in module.modules) {
            val result = build(nestedModule, scenario)
            if (result.failed) return result
        }


        val moduleResult = ModuleBuildResult()

        // now execute own build processes
        for (buildConfig in module.build.scenarios) {
            if (buildConfig.scenarios.any { it.matches(scenario) }) {
                for (step in buildConfig.rules) {
                    val context = BuildContext(scenario, module, step)
                    val result = step.execute(context)
                    moduleResult.append(result)
                    if (moduleResult.failed) break;
                }
                if (moduleResult.failed) break;
            }
        }

        modulesBuilt.put(module, moduleResult)

        return moduleResult
    }
}