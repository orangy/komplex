package komplex

import java.io.InputStream
import java.nio.file.Path

trait BuildStreamEndPoint : BuildEndPoint {
    val path : Path
    val inputStream : InputStream
}
