package komplex.kotlin

import komplex.*

val tools.kotlin = KotlinCompiler()
class KotlinCompiler  : Tool("kotlinc") {
    override fun execute(from: List<BuildEndPoint>, to: List<BuildEndPoint>) {
        println("Compiling...")
    }
}
