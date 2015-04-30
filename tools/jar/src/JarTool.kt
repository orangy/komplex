package komplex.tools.jar

import komplex.data.*
import java.io.*
import java.util.jar.*
import java.nio.file.Files
import java.nio.file.Path
import org.slf4j.LoggerFactory
import kotlin.properties.Delegates
import komplex.dsl
import komplex.model.BuildContext
import komplex.model.ArtifactDesc
import komplex.model.ArtifactData
import komplex.model.BuildResult
import komplex.dsl.FileArtifact
import komplex.dsl.FolderArtifact
import komplex.utils.findFilesInPath
import komplex.dsl.FileGlobArtifact
import komplex.utils.findGlobFiles
import komplex.utils.BuildDiagnostic
import java.util.*
import java.util.zip.ZipEntry

public val komplex.dsl.tools.jar: JarPackagerRule get() = JarPackagerRule(JarPackager())

val log = LoggerFactory.getLogger("komplex.tools.jar")

// separate class for separate class loading
// \todo check if moving to separate file or jar is needed for really lazy tool loading, or may be that nested class will work as well
public class JarPackagerRule(jarPackager: JarPackager) : komplex.dsl.BasicToolRule<JarPackagerRule, komplex.model.Tool<JarPackagerRule>>(jarPackager) {
    // configuration params
    public var deflate : Boolean = false
}

// compresses all sources into single destination described by the first target
// \todo add multiple targets and append support
public class JarPackager : komplex.model.Tool<JarPackagerRule> {
    override val name: String = "jar packager"

    // \todo add compression support
    private fun add(root: Path, sourcePath: Path, sourceData: InputStreamData, target: JarOutputStream, deflate: Boolean, entries: MutableSet<String>) {
        val entry = JarEntry(root.relativize(sourcePath).toString())
        //entry.setMethod(if (deflate) ZipEntry.DEFLATED else ZipEntry.STORED)
        //entry.setTime(source.lastModified())
        if (entries.add(entry.getName())) {
            log.trace("Adding entry ${entry.getName()}")
            target.putNextEntry(entry)
            val input = sourceData.inputStream
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
        else
            log.debug("Duplicate entry ${entry.getName()}")
    }

    // \todo add compression support
    private fun addFromJar(sourceJar: JarInputStream, target: JarOutputStream, deflate: Boolean, entries: MutableSet<String>) {
        val buffer = ByteArray(1024)
        while (true) {
            val entry = sourceJar.getNextEntry()
            if (entry == null) break
            //entry.setMethod(if (deflate) ZipEntry.DEFLATED else ZipEntry.STORED)
            if (entries.add(entry.getName())) {
                log.trace("Adding entry ${entry.getName()}")
                target.putNextEntry(entry)
                if (!entry.isDirectory()) {
                    var count = 0
                    while (true) {
                        count = sourceJar.read(buffer)
                        if (count < 0) break
                        target.write(buffer, 0, count)
                    }
                }
                target.closeEntry()
            }
            else
                log.debug("Duplicate entry ${entry.getName()}")
        }
    }

    private fun addFromJars(sourcePair: Pair<ArtifactDesc, ArtifactData?>, jarStream: JarOutputStream, deflate: Boolean, entries: HashSet<String>) {
        log.trace("Adding entries from jar(s) ${sourcePair.first.name}")
        val fs = openFileSet(sourcePair)
        for (file in fs.coll) {
            log.trace("Adding entries from jar file ${file.path}")
            addFromJar(JarInputStream(openInputStream(file).inputStream), jarStream, deflate, entries)
        }
    }

    override fun execute(context: BuildContext, cfg: JarPackagerRule, src: Iterable<Pair<ArtifactDesc, ArtifactData?>>, tgt: Iterable<ArtifactDesc>): BuildResult {

        val manifest = Manifest()
        manifest.getMainAttributes()?.put(Attributes.Name.MANIFEST_VERSION, "1.0")

        val targetDesc = tgt.single()
        val targetData =
                when (targetDesc) {
                    is FileArtifact -> {
                        log.info("$name to ${targetDesc.path}")
                        openOutputStream(targetDesc)
                    }
                    is FolderArtifact -> {
                        log.info("$name to ${targetDesc.path}")
                        FileOutputStreamData(targetDesc.path.resolve("${context.module.name}.jar")!!)
                    }
                    else -> throw IllegalArgumentException("$targetDesc is not supported in $name")
                }
        var jarStream = JarOutputStream(targetData.outputStream, manifest)

        val entries = hashSetOf<String>()

        for (sourcePair in src) {
            val sourceDesc = sourcePair.first
            when (sourceDesc) {
                // note: order matters
                // \todo consider redesign dispatching so order is not important any more
                is FileGlobArtifact ->
                    if (sourceDesc.type == komplex.dsl.artifacts.jar) addFromJars(sourcePair, jarStream, cfg.deflate, entries)
                    else komplex.data.openFileSet(sourcePair).coll.forEach { add(sourceDesc.path, it.path, openInputStream(it), jarStream, cfg.deflate, entries) }
                is FolderArtifact -> komplex.data.openFileSet(sourcePair).coll.forEach { add(sourceDesc.path, it.path, openInputStream(it), jarStream, cfg.deflate, entries) }
                is FileArtifact ->
                    if (sourceDesc.type == komplex.dsl.artifacts.jar) addFromJars(sourcePair, jarStream, cfg.deflate, entries)
                    else add(sourceDesc.path.getParent(), sourceDesc.path, openInputStream(sourcePair.second!!), jarStream, cfg.deflate, entries)
                else -> throw IllegalArgumentException("$sourceDesc is not supported in $name")
            }
        }
        jarStream.close()
        log.info("$name succeeded")
        return BuildResult(BuildDiagnostic.Success, listOf(Pair(targetDesc, targetData)))
    }
}

