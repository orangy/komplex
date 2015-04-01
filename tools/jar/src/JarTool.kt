package komplex.tools.jar

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
import komplex.data.InputStreamData
import komplex.dsl.FileArtifact
import komplex.dsl.FolderArtifact
import komplex.data.openOutputStream
import komplex.data.FileOutputStreamData
import komplex.utils.findFilesInPath
import komplex.data.openInputStream
import komplex.dsl.FileGlobArtifact
import komplex.utils.findGlobFiles
import komplex.utils.BuildDiagnostic

public val komplex.dsl.tools.jar: JarPackagerRule get() = JarPackagerRule(JarPackager())

val log = LoggerFactory.getLogger("komplex.tools.jar")

// separate class for separate class loading
// \todo check if moving to separate file or jar is needed for really lazy tool loading, or may be that nested class will work as well
public class JarPackagerRule(jarPackager: JarPackager) : komplex.dsl.BasicToolRule<JarPackagerRule, komplex.model.Tool<JarPackagerRule>>(jarPackager) {
    // configuration params
    public var compression : Int = 3
}

// compresses all sources into single destination described by the first target
// \todo add multiple targets and append support
public class JarPackager : komplex.model.Tool<JarPackagerRule> {
    override val name: String = "jar packager"

    // \todo add compression support
    private fun add(root: Path, sourcePath: Path, sourceData: InputStreamData, target: JarOutputStream, compression: Int) {
        val entry = JarEntry(root.relativize(sourcePath).toString())
        //entry.setTime(source.lastModified())
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

    override fun execute(context: BuildContext, cfg: JarPackagerRule, src: Iterable<Pair<ArtifactDesc, ArtifactData?>>, tgt: Iterable<ArtifactDesc>): BuildResult {
//    internal fun makePackage(from: Iterable<Artifact>, to: Iterable<Artifact>, rule: JarPackagerRule): BuildResult {
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

        for (sourcePair in src) {
            val sourceDesc = sourcePair.first
            when (sourceDesc) {
                is FileArtifact -> add(sourceDesc.path.getParent(), sourceDesc.path, openInputStream(sourcePair.second!!), jarStream, cfg.compression)
                is FolderArtifact -> komplex.data.openFileSet(sourcePair).coll.forEach { add(sourceDesc.path, it.path, openInputStream(it), jarStream, cfg.compression) }
                is FileGlobArtifact -> komplex.data.openFileSet(sourcePair).coll.forEach { add(sourceDesc.path, it.path, openInputStream(it), jarStream, cfg.compression) }
                else -> throw IllegalArgumentException("$sourceDesc is not supported in $name")
            }
        }
        jarStream.close()
        log.info("$name succeeded")
        return BuildResult(BuildDiagnostic.Success, listOf(Pair(targetDesc, targetData)))
    }
}

