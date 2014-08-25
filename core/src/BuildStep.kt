package komplex

public class BuildStep(val buildConfiguration : BuildConfiguration, val tool: Tool) {
    val sources = arrayListOf<BuildEndPoint>()
    val destinations = arrayListOf<BuildEndPoint>()

    public fun from(vararg endpoints: BuildEndPoint): BuildStep {
        sources.addAll(endpoints)
        return this
    }

    public fun to(vararg endpoints: BuildEndPoint): BuildStep {
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

    public val started: Event<BuildStep> = Event<BuildStep>("Started")
    public val finished: Event<BuildStep> = Event<BuildStep>("Finished")

    public fun execute(context : BuildContext) : BuildResult {
        started.fire(this)
        try {
            return tool.execute(context, sources, destinations)
        } catch (e : Throwable) {
            e.printStackTrace()
            return BuildResult.Fail
        } finally {
            finished.fire(this)
        }
    }
}
