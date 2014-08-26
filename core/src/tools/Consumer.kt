package komplex

public trait ConsumingTool : Tool {
    public val sources: List<BuildEndPoint>
    public fun addSources(vararg endpoints: BuildEndPoint)

    public override fun execute(context: BuildContext): BuildResult = consume(context, sources)
    protected fun consume(context: BuildContext, from: List<BuildEndPoint>): BuildResult

    override fun dump(indent: String) {
        println("$indent   From: ")
        for (endpoint in sources)
            endpoint.dump(indent + "    ")
    }
}

public abstract class Consumer(public override val title: String) : ConsumingTool {
    override val sources = arrayListOf<BuildEndPoint>()

    public override fun addSources(vararg endpoints: BuildEndPoint) {
        sources.addAll(endpoints)
    }
}

public fun <T : ConsumingTool> T.from(vararg endpoints: BuildEndPoint): T {
    addSources(*endpoints)
    return this
}


