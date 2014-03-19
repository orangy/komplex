package komplex


open class Tool(val title : String) {
    fun execute(from : Files, to : Files) {
        println("Running $title from $from to $to")
    }

}

object tools {
    val jar = Tool("Jar Packager")
    val kotlin = Tool("Kotlin Compiler")
}