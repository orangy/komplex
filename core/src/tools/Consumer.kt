package komplex

public trait ConsumingTool : Tool {
    public val sources: List<Artifact>
    public fun addSources(vararg endpoints: Artifact)

    public override fun execute(context: BuildStep): BuildResult = consume(context, sources)
    protected fun consume(context: BuildStep, from: List<Artifact>): BuildResult
}

public abstract class Consumer(public override val title: String) : ConsumingTool {
    override val sources = arrayListOf<Artifact>()

    public override fun addSources(vararg endpoints: Artifact) {
        sources.addAll(endpoints)
    }
}

public fun <T : ConsumingTool> T.from(vararg endpoints: Artifact): T {
    addSources(*endpoints)
    return this
}


