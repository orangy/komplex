package komplex.utils

import komplex.log
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

val fileSystem: FileSystem = FileSystems.getDefault()!!
internal val currentPath = Paths.get(System.getProperty("user.dir"))

fun Path?.orCurrent(): Path = this ?: currentPath

fun Path?.resolve(path: Path): Path = this.orCurrent().resolve(path)
fun Path?.resolve(path: String): Path = this.orCurrent().resolve(path)

fun Path?.relativize(path: Path): Path = this.orCurrent().relativize(path)
@Suppress("unused")
fun Path?.relativize(path: String): Path = this.orCurrent().relativize(path)

fun komplex.dsl.ScriptContext.resolvePath(path: Path): Path = this.env.rootDir.resolve(path)
fun komplex.dsl.ScriptContext.resolvePath(path: String): Path = this.env.rootDir.resolve(path)

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
    Files.walkFileTree(baseDir.resolve(path), Finder())
    return result
}

fun findGlobFiles(included: Iterable<String>, excluded: Iterable<String>, baseDir: Path? = null): List<Path> {
    val includeFilter = included.map { fileSystem.getPathMatcher("glob:$it") }
    val excludeFilter = excluded.map { fileSystem.getPathMatcher("glob:$it") }
    val basePath = baseDir?.normalize()
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

