package komplex

public abstract class Tool(public val title: String) {
    public abstract fun execute(context: BuildContext): BuildResult

    open fun dump(indent: String = "") {

    }
}

public abstract class ProcessingTool(title: String) : Tool(title) {
    val sources = arrayListOf<BuildEndPoint>()


    override fun execute(context: BuildContext): BuildResult = execute(context, sources)

    public abstract fun execute(context: BuildContext, from: List<BuildEndPoint>): BuildResult

    public fun from(vararg endpoints: BuildEndPoint): Tool {
        sources.addAll(endpoints)
        return this
    }

    override fun dump(indent: String) {
        super.dump(indent)
        println("$indent   From: ")
        for (endpoint in sources)
            endpoint.dump(indent + "    ")
    }
}

public abstract class ConvertingTool(title: String) : ProcessingTool(title) {
    val destinations = arrayListOf<BuildEndPoint>()

    override fun execute(context: BuildContext, from: List<BuildEndPoint>): BuildResult = execute(context, from, destinations)
    public abstract fun execute(context: BuildContext, from: List<BuildEndPoint>, to: List<BuildEndPoint>): BuildResult

    public fun to(vararg endpoints: BuildEndPoint): Tool {
        destinations.addAll(endpoints)
        return this
    }
    override fun dump(indent: String) {
        super.dump(indent)
        println("$indent   To: ")
        for (endpoint in destinations)
            endpoint.dump(indent + "    ")
    }

}

public object tools {}


