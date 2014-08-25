package komplex

import java.nio.file.*
import java.nio.file.attribute.*

public fun folder(path: String): FolderEndPoint = FolderEndPoint(fileSystem.getPath(path))
public class FolderEndPoint(val path: Path) : BuildFileSetEndPoint {
    override fun dump(indent: String) {
        println("$indent Folder ${path}")
    }

    override fun findFiles(baseDir: Path?): List<BuildStreamEndPoint> {
        val result = arrayListOf<BuildStreamEndPoint>()
        class Finder : SimpleFileVisitor<Path?>() {
            override fun visitFile(file: Path?, attrs: BasicFileAttributes): FileVisitResult {
                if (file != null) {
                    result.add(FileEndPoint(file))
                }
                return FileVisitResult.CONTINUE
            }
        }
        val dir = (baseDir ?: fileSystem.getPath("")).resolve(path)
        Files.walkFileTree(dir, Finder())
        return result
    }
}


