package komplex

import java.io.File
import java.nio.file.*
import java.nio.file.attribute.*

val fileSystem = FileSystems.getDefault()!!

trait BuildFileSetEndPoint : BuildEndPoint {
    fun findFiles(baseDir: Path? = null): List<BuildStreamEndPoint>
}

class GlobCollection(val collection: MutableList<String>) {
    fun path(value: String) {
        collection.add(value)
    }
}

fun files(glob: String) = FilesGlobEndPoint().let { it.include(glob); it }
class FilesGlobEndPoint : BuildFileSetEndPoint {
    val included = arrayListOf<String>()
    val excluded = arrayListOf<String>()

    override fun toString(): String {
        if (excluded.isEmpty())
            return "$included"
        else
            return "$included - $excluded"
    }

    fun invoke(body: FilesGlobEndPoint.() -> Unit) {
        this.body()
    }

    fun append(files : FilesGlobEndPoint) {
        included.addAll(files.included)
        excluded.addAll(files.excluded)
    }

    fun include(glob: String) {
        included.add(glob)
    }

    fun exclude(glob: String) {
        excluded.add(glob)
    }

    fun include(body: GlobCollection.() -> Unit) {
        val collection = GlobCollection(included)
        collection.body()
    }

    fun exclude(body: GlobCollection.() -> Unit) {
        val collection = GlobCollection(excluded)
        collection.body()
    }

    override fun findFiles(baseDir : Path?) : List<BuildStreamEndPoint> {
        val includeFilter = included map { fileSystem.getPathMatcher("glob:$it") }
        val excludeFilter = excluded map { fileSystem.getPathMatcher("glob:$it") }
        val result = arrayListOf<BuildStreamEndPoint>()

        class Finder : SimpleFileVisitor<Path?>() {
            override fun visitFile(file: Path?, attrs: BasicFileAttributes): FileVisitResult {
                if (file != null && includeFilter.any { it.matches(file)} && excludeFilter.none { it.matches(file) }) {
                    result.add(FileEndPoint(file))
                }
                return FileVisitResult.CONTINUE
            }
        }
        Files.walkFileTree(baseDir ?: fileSystem.getPath(""), Finder())
        return result
    }

    override fun dump(indent: String) {
        if (included.isNotEmpty()) {
            for (child in included) {
                println("$indent ${child}")
            }
        }
        if (excluded.isNotEmpty()) {
            println("$indent Excluded:")
            for (child in excluded) {
                println("$indent   ${child}")
            }
        }
        for (child in findFiles()) {
            println("$indent   File: ${child}")
        }
    }
}