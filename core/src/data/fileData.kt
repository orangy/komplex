package komplex.data

import java.nio.file.Path

public trait FileData : komplex.model.ArtifactData {
    public val path: Path
}

public open class SimpleFileData(override val path: Path) : FileData {}

public fun getFile(artifact: komplex.dsl.FileArtifact): FileData = SimpleFileData(artifact.path)