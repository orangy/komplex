package komplex

public class BuildPlan(val steps: List<BuildStep>) {

}

public fun BuildScript.plan(module: Module, scenario: Scenario): BuildPlan {
    val steps = arrayListOf<BuildStep>()

    for (ruleSet in module.build.ruleSets) {
        if (ruleSet.selectors.any { it.matches(scenario) }) {
            for (rule in ruleSet.rules) {
                val step = BuildStep(scenario, module, rule)
                planDependencies(step, steps)
                steps.add(step)
            }
        }
    }

    return BuildPlan(steps)
}


private fun BuildScript.planDependencies(step: BuildStep, to: MutableList<BuildStep>) {
    val moduleRule = step.rule
    if (moduleRule is ModuleToolRule<*>) {
        val tool = moduleRule.tool
        if (tool is ConsumingTool) {
            val sources = tool.sources
            for (artifact in sources) {
                val `type` = artifact.`type`

            }
         }
    }
}
