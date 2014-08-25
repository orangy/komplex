package komplex

public abstract class Tool(val title : String) {
    public abstract fun execute(context : BuildContext, from : List<BuildEndPoint>, to : List<BuildEndPoint>) : BuildResult
}

public object tools {}


