package komplex


abstract class Tool(val title : String) {
    abstract fun execute(process: BuildProcess, from : List<BuildEndPoint>, to : List<BuildEndPoint>) : BuildResult
}

object tools {}


