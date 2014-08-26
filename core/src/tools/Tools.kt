package komplex

public abstract class Tool(public val title: String) {
    public abstract fun execute(context: BuildContext): BuildResult

    open fun dump(indent: String = "") {

    }
}

public fun <T : Tool> T.with(body: T.() -> Unit): T {
    body()
    return this
}

public object tools {}


