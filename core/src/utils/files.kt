package komplex.utils

import komplex.log
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.FileVisitResult
import java.nio.file.Files

public val fileSystem: FileSystem = FileSystems.getDefault()!!

fun komplex.dsl.ScriptContext.resolvePath(path: Path): Path = this.env.rootDir?.resolve(path) ?: path
fun komplex.dsl.ScriptContext.resolvePath(path: String): Path = this.env.rootDir?.resolve(path) ?: fileSystem.getPath(path)

fun findFilesInPath(path: Path, baseDir: Path? = null): List<Path> {
    val result = arrayListOf<Path>()
    class Finder : SimpleFileVisitor<Path?>() {
        override fun visitFile(file: Path?, attrs: BasicFileAttributes): FileVisitResult {
            if (file != null) {
                result.add(file)
            }
            return FileVisitResult.CONTINUE
        }
    }
    val dir = (baseDir ?: fileSystem.getPath("")).resolve(path)
    Files.walkFileTree(dir, Finder())
    return result
}

public fun findGlobFiles(included: Iterable<String>, excluded: Iterable<String>, baseDir: Path? = null): List<Path> {
    val includeFilter = included map { fileSystem.getPathMatcher("glob:$it") }
    val excludeFilter = excluded map { fileSystem.getPathMatcher("glob:$it") }
    val basePath = (baseDir ?: fileSystem.getPath("")).normalize()
    val result = arrayListOf<Path>()

    class Finder : SimpleFileVisitor<Path?>() {
        override fun visitFile(file: Path?, attrs: BasicFileAttributes): FileVisitResult {
            if (file != null) {
                val relfile = basePath.relativize(file)
                if (includeFilter.any { it.matches(relfile) } && excludeFilter.none { it.matches(relfile) })
                    result.add(file)
            }
            return FileVisitResult.CONTINUE
        }
    }

    Files.walkFileTree(basePath, Finder())
    if (result.none())
        log.warn("No files found in the '$basePath' by the glob +[${included.joinToString(",")}] -[${excluded.joinToString(",")}")
    return result
}

