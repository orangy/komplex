package komplex

import java.util.*
import komplex.BuildGraph

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

    fun buildGraph(config: String = ""): BuildGraph = buildGraph(modules, Scenario(config))

    fun makeModuleSet(modules: List<Module>) {
        for (module in modules) {
            moduleReferences.put(module.moduleName, module)
            makeModuleSet(module.modules)
        }
    }

    fun build(module: Module, scenario: Scenario): BuildResult {
        val existingResult = modulesBuilt[module]
        if (existingResult != null)
            return existingResult

        // build dependencies
        for (it in module.depends.modules(scenario)) {
            val result = build(it, scenario)
            if (result.failed) return result
        }

        // build nested modules
        for (nestedModule in module.modules) {
            val result = build(nestedModule, scenario)
            if (result.failed) return result
        }

        val moduleResult = ModuleBuildResult()

        // now execute own build processes
        for (ruleSet in module.build.ruleSets) {
            if (ruleSet.selectors.any { it.matches(scenario) }) {
                for (step in ruleSet.rules) {
                    val context = BuildStepContext(scenario, module)
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