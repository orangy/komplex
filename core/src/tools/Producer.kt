package komplex

public trait ProducingTool : Tool {
    public val destinations : List<BuildEndPoint>
    public override fun execute(context: BuildContext): BuildResult = produce(context, destinations)
    protected fun produce(context: BuildContext, to: List<BuildEndPoint>): BuildResult
    public fun addDestinations(vararg endpoints: BuildEndPoint)

    override fun dump(indent: String) {
        println("$indent   To: ")
        for (endpoint in destinations)
            endpoint.dump(indent + "    ")
    }
}

public abstract class Producer(public override val title : String) : ProducingTool {
    public override val destinations = arrayListOf<BuildEndPoint>()

    override fun execute(context: BuildContext): BuildResult = execute(context, destinations)
    public abstract fun execute(context: BuildContext, to: List<BuildEndPoint>): BuildResult

    public override fun addDestinations(vararg endpoints: BuildEndPoint) {
        destinations.addAll(endpoints)
    }
}


public fun <T : ProducingTool> T.into(vararg endpoints: BuildEndPoint): T {
    addDestinations(*endpoints)
    return this
}
