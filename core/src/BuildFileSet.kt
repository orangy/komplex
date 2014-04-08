package komplex

import java.io.File
import java.nio.file.*
import java.nio.file.attribute.*

val fileSystem = FileSystems.getDefault()!!
fun String.toPath() = fileSystem.getPath(this)

class GlobCollection(val collection: MutableList<String>) {
    fun path(value: String) {
        collection.add(value)
    }
}

fun folder(path: String) = BuildFolder(path.toPath())
class BuildFolder(val path : Path) : BuildEndPoint {
    override fun dump(indent: String) {
        println("$indent Folder ${path}")
    }

    fun findFiles(baseDir: String = ""): List<Path> {
        val result = arrayListOf<Path>()
        class Finder : SimpleFileVisitor<Path?>() {
            override fun visitFile(file: Path?, attrs: BasicFileAttributes): FileVisitResult {
                if (file != null) {
                    result.add(file)
                }
                return FileVisitResult.CONTINUE
            }
        }
        Files.walkFileTree(baseDir.toPath(), Finder())
        return result
    }
}

fun file(path: String) = BuildFile(path.toPath())
class BuildFile(val path : Path) : BuildEndPoint {
    override fun dump(indent: String) {
        println("$indent File ${path}")
    }

    fun findFiles(baseDir: String = ""): List<Path> {
        return listOf(baseDir.toPath().resolve(path))
    }
}

fun files(glob: String) = BuildFileSet().let { it.include(glob); it }
class BuildFileSet : BuildEndPoint {
    val included = arrayListOf<String>()
    val excluded = arrayListOf<String>()

    override fun toString(): String {
        if (excluded.isEmpty())
            return "$included"
        else
            return "$included - $excluded"
    }

    fun invoke(body: BuildFileSet.() -> Unit) {
        this.body()
    }

    fun append(files : BuildFileSet) {
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

    fun findFiles(baseDir : String = "") : List<Path> {
        val includeFilter = included map { fileSystem.getPathMatcher("glob:$it") }
        val excludeFilter = excluded map { fileSystem.getPathMatcher("glob:$it") }
        val result = arrayListOf<Path>()

        class Finder : SimpleFileVisitor<Path?>() {
            override fun visitFile(file: Path?, attrs: BasicFileAttributes): FileVisitResult {
                if (file != null && includeFilter.any { it.matches(file)} && excludeFilter.none { it.matches(file) }) {
                    result.add(file)
                }
                return FileVisitResult.CONTINUE
            }
        }
        Files.walkFileTree(baseDir.toPath(), Finder())
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