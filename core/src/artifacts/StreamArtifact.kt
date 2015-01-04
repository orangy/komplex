package komplex

import java.io.*
import java.nio.file.Path

public trait StreamArtifact : Artifact {
    public val path: Path
    public val inputStream: InputStream
    public val outputStream: OutputStream
}

public fun MutableList<StreamArtifact>.addAll(endpoint: Artifact) {
    when (endpoint) {
        is StreamArtifact -> add(endpoint)
        is FileSetArtifact -> addAll(endpoint.findFiles())
        else -> throw IllegalArgumentException("Unknown endpoint: $endpoint")
    }
}

public fun Iterable<Artifact>.getAllStreams(): List<StreamArtifact> {
    val streams = arrayListOf<StreamArtifact>()
    for (item in this)
        streams.addAll(item)
    return streams
}


