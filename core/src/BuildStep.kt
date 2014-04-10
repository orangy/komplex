package komplex

class BuildStep(val buildConfiguration : BuildConfiguration, val tool: Tool) {
    val sources = arrayListOf<BuildEndPoint>()
    val destinations = arrayListOf<BuildEndPoint>()

    fun from(vararg endpoints: BuildEndPoint): BuildStep {
        sources.addAll(endpoints)
        return this
    }

    fun to(vararg endpoints: BuildEndPoint): BuildStep {
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

    val started = Event<BuildStep>("Started")
    val finished = Event<BuildStep>("Finished")

    fun execute(context : BuildContext) : BuildResult {
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
