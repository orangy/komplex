package komplex

import java.io.*
import java.nio.file.Path

public trait BuildStreamEndPoint : BuildEndPoint {
    public val path: Path
    public val inputStream: InputStream
    public val outputStream: OutputStream
}

public fun MutableList<BuildStreamEndPoint>.addAll(endpoint: BuildEndPoint) {
    when (endpoint) {
        is BuildStreamEndPoint -> add(endpoint)
        is BuildFileSetEndPoint -> addAll(endpoint.findFiles())
        else -> throw IllegalArgumentException("Unknown endpoint: $endpoint")
    }
}

public fun List<BuildEndPoint>.getAllStreams(): List<BuildStreamEndPoint> {
    val streams = arrayListOf<BuildStreamEndPoint>()
    for (item in this)
        streams.addAll(item)
    return streams
}


