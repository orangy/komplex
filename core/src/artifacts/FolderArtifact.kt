package komplex

import java.nio.file.*
import java.nio.file.attribute.*

public fun folder(path: String, `type`: ArtifactType): FolderArtifact = FolderArtifact(fileSystem.getPath(path), `type`)
public class FolderArtifact(public val path: Path, override val `type`: ArtifactType) : FileSetArtifact {
    override fun toString(): String = "$`type` folder ${path}"

    override fun findFiles(baseDir: Path?): List<StreamArtifact> {
        val result = arrayListOf<StreamArtifact>()
        class Finder : SimpleFileVisitor<Path?>() {
            override fun visitFile(file: Path?, attrs: BasicFileAttributes): FileVisitResult {
                if (file != null) {
                    result.add(FileArtifact(file, `type`))
                }
                return FileVisitResult.CONTINUE
            }
        }
        val dir = (baseDir ?: fileSystem.getPath("")).resolve(path)
        Files.walkFileTree(dir, Finder())
        return result
    }
}


