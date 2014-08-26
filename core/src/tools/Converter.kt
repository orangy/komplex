package komplex

public trait ConvertingTool : ProducingTool, ConsumingTool {
    override fun execute(context: BuildContext): BuildResult = convert(context, sources, destinations)
    override fun produce(context: BuildContext, to: List<BuildEndPoint>): BuildResult = convert(context, sources, to)
    override fun consume(context: BuildContext, from: List<BuildEndPoint>): BuildResult = convert(context, from, destinations)
    protected fun convert(context: BuildContext, from: List<BuildEndPoint>, to: List<BuildEndPoint>): BuildResult

    override fun dump(indent: String) {
        super<ConsumingTool>.dump(indent)
        super<ProducingTool>.dump(indent)
    }
}

public abstract class Converter(public override val title : String) : ConvertingTool {
    public override val sources = arrayListOf<BuildEndPoint>()
    public override val destinations = arrayListOf<BuildEndPoint>()

    public override fun addSources(vararg endpoints: BuildEndPoint) {
        sources.addAll(endpoints)
    }

    public override fun addDestinations(vararg endpoints: BuildEndPoint) {
        destinations.addAll(endpoints)
    }
}



