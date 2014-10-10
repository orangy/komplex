package komplex

import java.nio.file.*
import java.nio.file.attribute.*

public val fileSystem: FileSystem = FileSystems.getDefault()!!

public trait FileSetArtifact : Artifact {
    public fun findFiles(baseDir: Path? = null): List<StreamArtifact>
}

public class GlobCollection(val collection: MutableList<String>) {
    public fun path(value: String) {
        collection.add(value)
    }
}

public fun files(glob: String, `type`: ArtifactType): FileSetArtifact = FileGlobArtifact(`type`).let { it.include(glob); it }
class FileGlobArtifact(override val `type`: ArtifactType) : FileSetArtifact {
    val included = arrayListOf<String>()
    val excluded = arrayListOf<String>()

    public fun invoke(body: FileGlobArtifact.() -> Unit) {
        this.body()
    }

    public fun append(files: FileGlobArtifact) {
        included.addAll(files.included)
        excluded.addAll(files.excluded)
    }

    public fun include(glob: String) {
        included.add(glob)
    }

    public fun exclude(glob: String) {
        excluded.add(glob)
    }

    public fun include(body: GlobCollection.() -> Unit) {
        val collection = GlobCollection(included)
        collection.body()
    }

    public fun exclude(body: GlobCollection.() -> Unit) {
        val collection = GlobCollection(excluded)
        collection.body()
    }

    public override fun findFiles(baseDir: Path?): List<StreamArtifact> {
        val includeFilter = included map { fileSystem.getPathMatcher("glob:$it") }
        val excludeFilter = excluded map { fileSystem.getPathMatcher("glob:$it") }
        val result = arrayListOf<StreamArtifact>()

        class Finder : SimpleFileVisitor<Path?>() {
            override fun visitFile(file: Path?, attrs: BasicFileAttributes): FileVisitResult {
                if (file != null && includeFilter.any { it.matches(file) } && excludeFilter.none { it.matches(file) }) {
                    result.add(FileArtifact(file, `type`))
                }
                return FileVisitResult.CONTINUE
            }
        }
        Files.walkFileTree(baseDir ?: fileSystem.getPath(""), Finder())
        return result
    }

    override fun toString(): String = "$`type` glob +$included -$excluded"
}