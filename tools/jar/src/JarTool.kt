package komplex.jar

import komplex.*

val tools.jar = JarPackager()
class JarPackager  : Tool("Jar Packager") {
    override fun execute(from: Files, to: Files) {
        println("Building jar...")
    }
}

