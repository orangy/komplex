package komplex

public trait Tool {
    val title: String
    public fun execute(context: BuildStep): BuildResult
}

public fun <T : Tool> T.with(body: T.() -> Unit): T {
    body()
    return this
}

public object tools {}
