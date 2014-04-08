package komplex


abstract class Tool(val title : String) {
    abstract fun execute(from : List<BuildEndPoint>, to : List<BuildEndPoint>)
}

object tools {}


