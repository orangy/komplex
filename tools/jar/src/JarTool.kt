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

    fun MutableList<BuildStreamEndPoint>.addAll(endpoint : BuildEndPoint) {
        when (endpoint) {
            is BuildStreamEndPoint -> add(endpoint)
            is BuildFileSetEndPoint -> addAll(endpoint.findFiles())
            else -> throw IllegalArgumentException("Unknown endpoint: $endpoint")
        }
    }

    override fun execute(from: List<BuildEndPoint>, to: List<BuildEndPoint>) {
        val manifest = Manifest()
        manifest.getMainAttributes()?.put(Attributes.Name.MANIFEST_VERSION, "1.0")
        val target = JarOutputStream(FileOutputStream("output.jar"), manifest)

        val streams = arrayListOf<BuildStreamEndPoint>()
        for (item in from)
            streams.addAll(item)

        for (stream in streams)
            add(stream, target)
        target.close()
    }
}

