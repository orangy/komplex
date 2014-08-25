package komplex

public class ModuleScenarioRule(val buildConfiguration: ModuleScenarioRules, val tool: Tool) {
    val sources = arrayListOf<BuildEndPoint>()
    val destinations = arrayListOf<BuildEndPoint>()

    public fun from(vararg endpoints: BuildEndPoint): ModuleScenarioRule {
        sources.addAll(endpoints)
        return this
    }

    public fun to(vararg endpoints: BuildEndPoint): ModuleScenarioRule {
        destinations.addAll(endpoints)
        return this
    }

    fun dump(indent: String = "") {
        println("$indent ${tool.title}")

        println("$indent   From: ")
        for (endpoint in sources)
            endpoint.dump(indent + "    ")
        println("$indent   To: ")
        for (endpoint in destinations)
            endpoint.dump(indent + "    ")
    }

    public fun execute(context: BuildContext): BuildResult {
        try {
            return tool.execute(context, sources, destinations)
        } catch (e: Throwable) {
            e.printStackTrace()
            return BuildResult.Fail
        }
    }
}
