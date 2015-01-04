package komplex.jar

import komplex.*
import java.io.*
import java.util.jar.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.properties.Delegates

public val tools.jar: JarPackagerRule
    get() = JarPackagerRule()

// separate class for separate class loading
// \todo check if moving to separate file or jar is needed for really lazy tool loading, or may be that nested class will work as well
public class JarPackagerRule(override val local: Boolean = false) : Converter.BaseRule(local) {
    override val tool: JarPackager by Delegates.lazy { JarPackager() }
    // configuration params
    public var compression : Int = 3
    // executor
    override fun execute(context: BuildStepContext): BuildResult
            = tool.makePackage(context, sources(context.scenario), targets(context.scenario), this)
}

public class JarPackager : Converter("Jar Packager") {
    override fun convert(context: BuildStepContext, from: Iterable<Artifact>, to: Iterable<Artifact>): BuildResult = null!!

    // \todo add compression support
    private fun add(root: Path, source: StreamArtifact, target: JarOutputStream, compression: Int) {
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

    internal fun makePackage(context: BuildStepContext, from: Iterable<Artifact>, to: Iterable<Artifact>, rule: JarPackagerRule): BuildResult {
        val manifest = Manifest()
        manifest.getMainAttributes()?.put(Attributes.Name.MANIFEST_VERSION, "1.0")

        val destination = to.single()
        val target =
                when (destination) {
                    is StreamArtifact -> {
                        println("[INFO] $title to ${destination.path}")
                        destination.path.getParent()?.let { Files.createDirectories(it) }
                        JarOutputStream(destination.outputStream, manifest)
                    }
                    is FolderArtifact -> {
                        println("[INFO] $title to ${destination.path}")
                        Files.createDirectories(destination.path)
                        JarOutputStream(FileOutputStream(destination.path.resolve("${context.module.moduleName}.jar")!!.toFile()), manifest)
                    }
                    else -> throw IllegalArgumentException("$destination is not supported in $title")
                }

        for (source in from) {
            when (source) {
                is FolderArtifact -> {
                    for (item in source.findFiles())
                        add(source.path, item, target, rule.compression)
                }
                else -> throw IllegalArgumentException("$source is not supported in $title")
            }
        }
        target.close()
        println("[INFO] $title succeeded")
        return BuildResult.Success
    }
}

