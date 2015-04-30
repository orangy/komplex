package komplex.data

import java.io.File
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.properties.Delegates

public fun fileHash(path: Path): ByteArray {
    val digest = MessageDigest.getInstance("SHA-1")
    path.toFile().forEachBlock { bytes, i -> digest.update(bytes) }
    return digest.digest()
}

public trait FileData : komplex.model.ArtifactData {
    public val path: Path
}

public open class SimpleFileData(ipath: Path) : FileData {
    override val path: Path = ipath.normalize().toAbsolutePath()
    override val sourcesHash: ByteArray? = null
    override val hash: ByteArray by Delegates.lazy { fileHash(path) }
}

public fun getFile(artifact: komplex.dsl.FileArtifact): FileData = SimpleFileData(artifact.path)

public class FolderData(ipath: Path) : FileData {
    override val path: Path = ipath.normalize().toAbsolutePath()
    override val sourcesHash: ByteArray? = null
    override val hash: ByteArray by Delegates.lazy { mergeHashes(collectFolderFiles(path)) }
}
