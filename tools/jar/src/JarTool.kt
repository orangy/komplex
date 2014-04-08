package komplex.jar

import komplex.*

val tools.jar = JarPackager()
class JarPackager  : Tool("Jar Packager") {
    override fun execute(from: List<BuildEndPoint>, to: List<BuildEndPoint>) {
        println("Building jar...")
    }
}

