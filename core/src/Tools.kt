package komplex


abstract class Tool(val title : String) {
    abstract fun execute(from : Files, to : Files)
}

object tools {}


