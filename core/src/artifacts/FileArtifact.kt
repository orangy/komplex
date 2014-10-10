package komplex

import java.nio.file.Path
import java.io.*

public fun file(path: String, `type`: ArtifactType): FileArtifact = FileArtifact(fileSystem.getPath(path), `type`)

public class FileArtifact(public override val path: Path, override val `type`: ArtifactType) : StreamArtifact {

    override val inputStream: InputStream
        get() = BufferedInputStream(FileInputStream(path.toFile()))

    override val outputStream: OutputStream
        get() = BufferedOutputStream(FileOutputStream(path.toFile()))

    override fun toString(): String = "$`type` file ${path}"
}

