package komplex

import java.nio.file.Path
import java.io.*

fun file(path: String) = BuildFile(fileSystem.getPath(path))
class BuildFile(override val path : Path) : BuildStreamEndPoint {

    override val inputStream: InputStream
        get() = BufferedInputStream(FileInputStream(path.toFile()))

    override fun dump(indent: String) {
        println("$indent File ${path}")
    }

    override fun toString(): String = "$path"
}

