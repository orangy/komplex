package komplex.kotlin

import komplex.*

val tools.kotlin = KotlinCompiler()
class KotlinCompiler  : Tool("kotlinc") {
    override fun execute(from: Files, to: Files) {
        println("Compiling...")
    }
}
