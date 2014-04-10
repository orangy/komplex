package komplex.jar

import komplex.*
import java.io.*
import java.util.jar.*
import java.nio.file.*

val tools.jar = JarPackager()
class JarPackager : Tool("Jar Packager") {
    private fun add(source: BuildStreamEndPoint, target: JarOutputStream) {
        val entry = JarEntry(source.path.toString())
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
            is BuildStreamEndPoint -> JarOutputStream(destination.outputStream, manifest)
            is BuildFolder -> JarOutputStream(FileOutputStream(destination.path.resolve("output.jar")!!.toFile()), manifest)
            else -> throw IllegalArgumentException("$destination is not supported in copy tool")
        }

        for (stream in from.getAllStreams())
            add(stream, target)
        target.close()
        return BuildResult.Success
    }
}

