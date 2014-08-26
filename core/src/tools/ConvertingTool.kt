package komplex

public abstract class ConvertingTool(title: String) : ConsumingTool(title) {
    val destinations = arrayListOf<BuildEndPoint>()

    override fun execute(context: BuildContext, from: List<BuildEndPoint>): BuildResult = execute(context, from, destinations)
    public abstract fun execute(context: BuildContext, from: List<BuildEndPoint>, to: List<BuildEndPoint>): BuildResult

    public fun addDestinations(vararg endpoints: BuildEndPoint) {
        destinations.addAll(endpoints)
    }

    override fun dump(indent: String) {
        super.dump(indent)
        println("$indent   To: ")
        for (endpoint in destinations)
            endpoint.dump(indent + "    ")
    }
}

public fun <T : ConvertingTool> T.into(vararg endpoints: BuildEndPoint): T {
    addDestinations(*endpoints)
    return this
}


