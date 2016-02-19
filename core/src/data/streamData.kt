package komplex.data

import komplex.dsl.FileArtifact
import komplex.model.ArtifactData
import java.io.*
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

interface InputStreamData : komplex.model.ArtifactData {
    val inputStream: InputStream
}

interface OutputStreamData : komplex.model.ArtifactData {
    val outputStream: OutputStream
}


open class FileInputStreamData(val path: Path) : InputStreamData {
    override val hash: ByteArray by lazy { fileHash(path) }
    override val inputStream: InputStream
        get() = BufferedInputStream(FileInputStream(path.toFile()))
}


class BufferedOutputStreamWithHash(strm: OutputStream): BufferedOutputStream(strm) {
    val digest = MessageDigest.getInstance("SHA-1")
    val hash: ByteArray get() = digest.digest()

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


open class FileOutputStreamData(val path: Path) : OutputStreamData {
    override val hash: ByteArray get() = outputStream.hash
    override val outputStream: BufferedOutputStreamWithHash
        get() {
            path.parent.let { Files.createDirectories(it) }
            return BufferedOutputStreamWithHash(FileOutputStream(path.toFile()))
        }
}


fun openInputStream(artifact: FileArtifact): InputStreamData = FileInputStreamData(artifact.path)

fun openInputStream(data: FileData): InputStreamData = FileInputStreamData(data.path)

@Suppress("USELESS_CAST")
fun openInputStream(data: ArtifactData): InputStreamData =
        when (data) {
            is FileArtifact -> openInputStream(data as FileArtifact)
            is FileData -> openInputStream(data as FileData)
            is FileOutputStreamData -> FileInputStreamData(data.path)
            else -> throw UnsupportedOperationException("Cannot open input strream from '$data'")
        }


fun openOutputStream(data: FileData): OutputStreamData = FileOutputStreamData(data.path)

fun openOutputStream(artifact: FileArtifact): OutputStreamData = FileOutputStreamData(artifact.path)

// \todo check kotlin dispatching rules
@Suppress("USELESS_CAST")
fun openOutputStream(data: ArtifactData): OutputStreamData =
        when (data) {
            is FileArtifact -> openOutputStream(data as FileArtifact)
            is FileData -> openOutputStream(data as FileData)
            else -> throw UnsupportedOperationException("Cannot open output strream from '$data'")
        }

