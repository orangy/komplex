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
import java.nio.ByteBuffer
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.properties.Delegates

public trait InputStreamData : komplex.model.ArtifactData {
    public val inputStream: InputStream
}

public trait OutputStreamData : komplex.model.ArtifactData {
    public val outputStream: OutputStream
}


public open class FileInputStreamData(public val path: Path) : InputStreamData {
    override val hash: ByteArray by Delegates.lazy { fileHash(path) }
    override val inputStream: InputStream
        get() = BufferedInputStream(FileInputStream(path.toFile()))
}


class BufferedOutputStreamWithHash(strm: OutputStream): BufferedOutputStream(strm) {
    val digest = MessageDigest.getInstance("SHA-1")
    public val hash: ByteArray get() = digest.digest()

    override fun write(b: Int) {
        super.write(b)
        digest.update(ByteBuffer.allocate(4).putInt(b).array())
    }

    override fun write(b: ByteArray?, off: Int, len: Int) {
        super.write(b, off, len)
        digest.update(b, off, len)
    }

    override fun write(b: ByteArray?) {
        super.write(b)
        digest.update(b)
    }

}


public open class FileOutputStreamData(public val path: Path) : OutputStreamData {
    override val hash: ByteArray get() = outputStream.hash
    override val outputStream: BufferedOutputStreamWithHash
        get() {
            path.getParent()?.let { Files.createDirectories(it) }
            return BufferedOutputStreamWithHash(FileOutputStream(path.toFile()))
        }
}


public fun openInputStream(artifact: FileArtifact): InputStreamData = FileInputStreamData(artifact.path)
public fun openInputStream(data: FileData): InputStreamData = FileInputStreamData(data.path)
public fun openInputStream(data: ArtifactData): InputStreamData =
        when (data) {
            is FileArtifact -> openInputStream(data as FileArtifact)
            is FileData -> openInputStream(data as FileData)
            is FileOutputStreamData -> FileInputStreamData(data.path)
            else -> throw UnsupportedOperationException("Cannot open input strream from '$data'")
        }

public fun openOutputStream(data: FileData): OutputStreamData = FileOutputStreamData(data.path)
public fun openOutputStream(artifact: FileArtifact): OutputStreamData = FileOutputStreamData(artifact.path)
// \todo check kotlin dispatching rules
public fun openOutputStream(data: ArtifactData): OutputStreamData =
        when (data) {
            is FileArtifact -> openOutputStream(data as FileArtifact)
            is FileData -> openOutputStream(data as FileData)
            else -> throw UnsupportedOperationException("Cannot open output strream from '$data'")
        }

