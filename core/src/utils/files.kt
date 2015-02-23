package komplex.utils

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.FileVisitResult
import java.nio.file.Files

public val fileSystem: FileSystem = FileSystems.getDefault()!!

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
    val result = arrayListOf<Path>()

    class Finder : SimpleFileVisitor<Path?>() {
        override fun visitFile(file: Path?, attrs: BasicFileAttributes): FileVisitResult {
            if (file != null && includeFilter.any { it.matches(file) } && excludeFilter.none { it.matches(file) }) {
                result.add(file)
            }
            return FileVisitResult.CONTINUE
        }
    }
    Files.walkFileTree(baseDir ?: fileSystem.getPath(""), Finder())
    return result
}

