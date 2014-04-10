package komplex

class BuildConfiguration(val buildProject: BuildProject, val configurations : List<Config>) {
    val steps = arrayListOf<BuildStep>()

    fun invoke(body : BuildConfiguration.()->Unit) {
        body()
    }

    fun using(tool: Tool): BuildStep {
        val process = BuildStep(this, tool)
        steps.add(process)
        return process
    }

    fun dump(indent: String = "") {
        println("$indent Build $configurations")
        for (step in steps) {
            step.dump(indent + "  ")
        }
    }
}

