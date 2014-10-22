package komplex

public trait ProducingTool : Tool {
    public val destinations : List<Artifact>
    public override fun execute(context: BuildStep): BuildResult = produce(context, destinations)
    protected fun produce(context: BuildStep, to: List<Artifact>): BuildResult
    public fun addDestinations(vararg endpoints: Artifact)
}

public abstract class Producer(public override val title : String) : ProducingTool {
    public override val destinations = arrayListOf<Artifact>()

    override fun execute(context: BuildStep): BuildResult = execute(context, destinations)
    public abstract fun execute(context: BuildStep, to: List<Artifact>): BuildResult

    public override fun addDestinations(vararg endpoints: Artifact) {
        destinations.addAll(endpoints)
    }
}

public fun <T : ProducingTool> T.into(vararg endpoints: Artifact): T {
    addDestinations(*endpoints)
    return this
}
