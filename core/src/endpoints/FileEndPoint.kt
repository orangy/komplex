package komplex

import java.nio.file.Path
import java.io.*

fun file(path: String) = FileEndPoint(fileSystem.getPath(path))
class FileEndPoint(override val path : Path) : BuildStreamEndPoint {

    override val inputStream: InputStream
        get() = BufferedInputStream(FileInputStream(path.toFile()))

    override val outputStream: OutputStream
        get() = BufferedOutputStream(FileOutputStream(path.toFile()))

    override fun dump(indent: String) {
        println("$indent File ${path}")
    }

    override fun toString(): String = "$path"
}

