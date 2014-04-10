package komplex

abstract class Tool(val title : String) {
    abstract fun execute(context : BuildContext, from : List<BuildEndPoint>, to : List<BuildEndPoint>) : BuildResult
}

object tools {}


