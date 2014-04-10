package komplex

import java.io.*
import java.nio.file.Path

trait BuildStreamEndPoint : BuildEndPoint {
    val path: Path
    val inputStream: InputStream
    val outputStream: OutputStream
}

fun MutableList<BuildStreamEndPoint>.addAll(endpoint: BuildEndPoint) {
    when (endpoint) {
        is BuildStreamEndPoint -> add(endpoint)
        is BuildFileSetEndPoint -> addAll(endpoint.findFiles())
        else -> throw IllegalArgumentException("Unknown endpoint: $endpoint")
    }
}

fun List<BuildEndPoint>.getAllStreams(): List<BuildStreamEndPoint> {
    val streams = arrayListOf<BuildStreamEndPoint>()
    for (item in this)
        streams.addAll(item)
    return streams
}


