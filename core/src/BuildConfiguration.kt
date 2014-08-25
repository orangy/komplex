package komplex

public class BuildConfiguration(val buildModule: BuildModule, val configurations : List<Config>) {
    val steps = arrayListOf<BuildStep>()

    public fun invoke(body : BuildConfiguration.()->Unit) {
        body()
    }

    public fun using(tool: Tool): BuildStep {
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

