package komplex

public trait ConvertingTool : ProducingTool, ConsumingTool {
    override fun execute(context: BuildContext): BuildResult = convert(context, sources, destinations)
    override fun produce(context: BuildContext, to: List<Artifact>): BuildResult = convert(context, sources, to)
    override fun consume(context: BuildContext, from: List<Artifact>): BuildResult = convert(context, from, destinations)
    protected fun convert(context: BuildContext, from: List<Artifact>, to: List<Artifact>): BuildResult
}

public abstract class Converter(public override val title : String) : ConvertingTool {
    public override val sources = arrayListOf<Artifact>()
    public override val destinations = arrayListOf<Artifact>()

    public override fun addSources(vararg endpoints: Artifact) {
        sources.addAll(endpoints)
    }

    public override fun addDestinations(vararg endpoints: Artifact) {
        destinations.addAll(endpoints)
    }
}



