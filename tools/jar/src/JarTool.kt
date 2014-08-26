package komplex.jar

import komplex.*
import java.io.*
import java.util.jar.*
import java.nio.file.Files
import java.nio.file.Path

public val tools.jar: JarPackager
    get() = JarPackager()

public class JarPackager : ConvertingTool("Jar Packager") {
    public var compression : Int = 3

    private fun add(root: Path, source: BuildStreamEndPoint, target: JarOutputStream) {
        val entry = JarEntry(root.relativize(source.path).toString())
        //entry.setTime(source.lastModified())
        target.putNextEntry(entry)
        val input = source.inputStream
        try {
            val buffer = ByteArray(1024)
            while (true) {
                val count = input.read(buffer)
                if (count == -1)
                    break
                target.write(buffer, 0, count)
            }
            target.closeEntry()
        } finally {
            input.close()
        }
    }

    override fun execute(context: BuildContext, from: List<BuildEndPoint>, to: List<BuildEndPoint>): BuildResult {
        val manifest = Manifest()
        manifest.getMainAttributes()?.put(Attributes.Name.MANIFEST_VERSION, "1.0")

        val destination = to.single()
        val target =
                when (destination) {
                    is BuildStreamEndPoint -> {
                        destination.path.getParent()?.let { Files.createDirectories(it) }
                        JarOutputStream(destination.outputStream, manifest)
                    }
                    is FolderEndPoint -> {
                        Files.createDirectories(destination.path)
                        JarOutputStream(FileOutputStream(destination.path.resolve("${context.module.moduleName}.jar")!!.toFile()), manifest)
                    }
                    else -> throw IllegalArgumentException("$destination is not supported in $title")
                }

        for (source in from) {
            when (source) {
                is FolderEndPoint -> {
                    for (item in source.findFiles())
                        add(source.path, item, target)
                }
                else -> throw IllegalArgumentException("$source is not supported in $title")
            }
        }
        target.close()
        return BuildResult.Success
    }
}

