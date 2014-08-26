package komplex

public abstract class ConsumingTool(title: String) : Tool(title) {
    val sources = arrayListOf<BuildEndPoint>()

    override fun execute(context: BuildContext): BuildResult = execute(context, sources)

    public abstract fun execute(context: BuildContext, from: List<BuildEndPoint>): BuildResult

    public fun addSources(vararg endpoints: BuildEndPoint) {
        sources.addAll(endpoints)
    }

    override fun dump(indent: String) {
        super.dump(indent)
        println("$indent   From: ")
        for (endpoint in sources)
            endpoint.dump(indent + "    ")
    }
}

public fun <T : ConsumingTool> T.from(vararg endpoints: BuildEndPoint): T {
    addSources(*endpoints)
    return this
}


