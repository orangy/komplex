package komplex.data

import java.io.InputStream
import java.io.OutputStream
import komplex.dsl.FileArtifact
import java.io.FileInputStream
import java.io.BufferedInputStream
import java.nio.file.Path
import java.io.FileOutputStream
import java.io.BufferedOutputStream
import komplex.model.ArtifactData
import java.nio.file.Files

public trait InputStreamData : komplex.model.ArtifactData {
    public val inputStream: InputStream
}

public trait OutputStreamData : komplex.model.ArtifactData {
    public val outputStream: OutputStream
}


public open class FileInputStreamData(val path: Path) : InputStreamData {
    override val inputStream: InputStream
        get() = BufferedInputStream(FileInputStream(path.toFile()))
}

public open class FileOutputStreamData(val path: Path) : OutputStreamData {
    override val outputStream: OutputStream
        get() {
            path.getParent()?.let { Files.createDirectories(it) }
            return BufferedOutputStream(FileOutputStream(path.toFile()))
        }
}


public fun openInputStream(artifact: FileArtifact): InputStreamData = FileInputStreamData(artifact.path)
public fun openInputStream(data: FileData): InputStreamData = FileInputStreamData(data.path)
// \todo check kotlin dispatching rules
public fun openInputStream(unknown: ArtifactData): InputStreamData { throw UnsupportedOperationException("Cannot open input strream from '$unknown'") }

public fun openOutputStream(data: FileData): OutputStreamData = FileOutputStreamData(data.path)
public fun openOutputStream(artifact: FileArtifact): OutputStreamData = FileOutputStreamData(artifact.path)
// \todo check kotlin dispatching rules
public fun openOutputStream(unknown: ArtifactData): OutputStreamData { throw UnsupportedOperationException("Cannot open output strream from '$unknown'") }
