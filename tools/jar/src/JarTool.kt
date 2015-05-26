package komplex.tools.jar

import komplex.data.*
import java.io.*
import java.util.jar.*
import java.nio.file.Files
import java.nio.file.Path
import org.slf4j.LoggerFactory
import kotlin.properties.Delegates
import komplex.dsl
import komplex.dsl.*
import komplex.model.BuildContext
import komplex.model.ArtifactDesc
import komplex.model.ArtifactData
import komplex.model.BuildResult
import komplex.tools.filterIn
import komplex.utils.findFilesInPath
import komplex.utils.findGlobFiles
import komplex.utils.BuildDiagnostic
import java.nio.file.Paths
import java.util.*
import java.util.zip.ZipEntry

public val komplex.dsl.tools.jar: JarPackagerRule get() = JarPackagerRule(JarPackager())

val log = LoggerFactory.getLogger("komplex.tools.jar")

public data class JarManifestProperty(public val name: String, public val value: String) {}

// separate class for separate class loading
// \todo check if moving to separate file or jar is needed for really lazy tool loading, or may be that nested class will work as well
public class JarPackagerRule(jarPackager: JarPackager) : komplex.dsl.BasicToolRule<JarPackagerRule, komplex.model.Tool<JarPackagerRule>>(jarPackager) {

    // this is not very flexible and quite expensive scheme
    // \todo implement generic sources/target pairs within the same rule and ability to assign attributes to targets
    internal val explicitPrefixedFroms: MutableMap<String, RuleSources> = hashMapOf()
    override val fromSources: Iterable<ArtifactDesc> get() = super.fromSources + explicitPrefixedFroms.values().flatMap { it.collect(selector.scenarios) }
    // configuration params
    public var deflate : Boolean = false
    public val manifest: MutableCollection< () -> JarManifestProperty> = arrayListOf()
}

public fun <S: GenericSourceType> JarPackagerRule.from(args: Iterable<S>, prefix: String): JarPackagerRule =
        addToSources(explicitPrefixedFroms.getOrPut(prefix, { RuleSources() }), args)
public fun <S: GenericSourceType> JarPackagerRule.from(vararg args: S, prefix: String): JarPackagerRule =
        addToSources(explicitPrefixedFroms.getOrPut(prefix, { RuleSources() }), *args)

public fun JarPackagerRule.addManifestProperty(name: String, generator: () -> String): JarPackagerRule {
    manifest.add({ JarManifestProperty(name, generator()) })
    return this
}

public fun JarPackagerRule.addManifestProperty(name: String, value: String): JarPackagerRule {
    manifest.add({ JarManifestProperty(name, value) })
    return this
}

// compresses all sources into single destination described by the first target
// \todo add multiple targets and append support
public class JarPackager : komplex.model.Tool<JarPackagerRule> {
    override val name: String = "jar packager"

    private fun prefixPath(prefix: String?, path: Path): Path {
        assert( !path.isAbsolute())
        return if (prefix != null && prefix.length() > 0)
                    Paths.get(prefix, path.toString())
               else path
    }

    private fun addDirs(prefix: String?, root: Path, sourcePath: Path, target: JarOutputStream, deflate: Boolean, entries: MutableSet<String>) {
        if (!root.equals(sourcePath)) {
            val entry = JarEntry( prefixPath(prefix, root.relativize(sourcePath)).toString() + "/")
            if (entries.add(entry.getName())) {
                log.trace("Adding directory entry ${entry.getName()}")
                target.putNextEntry(entry)
                target.closeEntry()
            }
            addDirs(prefix, root, sourcePath.getParent(), target, deflate, entries)
        }
    }

    // \todo add compression support
    private fun add(prefix: String?, root: Path, sourcePath: Path, sourceData: InputStreamData, target: JarOutputStream, deflate: Boolean, entries: MutableSet<String>) {
        addDirs(prefix, root, sourcePath.getParent(), target, deflate, entries)
        val entry = JarEntry( prefixPath(prefix, root.relativize(sourcePath)).toString())
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
    private fun addFromJar(prefix: String?, sourceJar: JarInputStream, target: JarOutputStream, deflate: Boolean, entries: MutableSet<String>) {
        val buffer = ByteArray(1024)
        while (true) {
            var entry: ZipEntry = sourceJar.getNextEntry() ?: break
            if (prefix != null && prefix.length() > 0) {
                // placing entry to another place
                // \todo try to find more elegant way to change entry name
                val newEntry = JarEntry(Paths.get(prefix, entry.getName()).toString())
                newEntry.setComment(entry.getComment())
                newEntry.setCompressedSize(entry.getCompressedSize())
                newEntry.setCrc(entry.getCrc())
                newEntry.setExtra(entry.getExtra())
                newEntry.setMethod(entry.getMethod())
                newEntry.setSize(entry.getSize())
                newEntry.setTime(entry.getTime())
                entry = newEntry
            }
            //entry.setMethod(if (deflate) ZipEntry.DEFLATED else ZipEntry.STORED)
            if (entries.add(entry.getName())) {
                log.trace("  ${entry.getName()}")
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

    private fun addFromJars(prefix: String?, sourcePair: Pair<ArtifactDesc, ArtifactData?>, jarStream: JarOutputStream, deflate: Boolean, entries: HashSet<String>) {
        log.trace("Adding entries from jar(s) ${sourcePair.first.name}")
        val fs = openFileSet(sourcePair)
        for (file in fs.coll) {
            log.trace("Adding entries from jar file ${file.path}")
            addFromJar(prefix, JarInputStream(openInputStream(file).inputStream), jarStream, deflate, entries)
        }
    }

    override fun execute(context: BuildContext, cfg: JarPackagerRule, src: Iterable<Pair<ArtifactDesc, ArtifactData?>>, tgt: Iterable<ArtifactDesc>): BuildResult {

        val manifest = Manifest()
        val manifestAttrs = manifest.getMainAttributes()!!
        manifestAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.0")
        cfg.manifest.forEach { val prop = it(); log.debug("adding manifest property ${prop.name} = ${prop.value}"); manifestAttrs.put(Attributes.Name(prop.name), prop.value) }

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
        //jarStream.setMethod()
        jarStream.setMethod(if (cfg.deflate) ZipEntry.DEFLATED else ZipEntry.STORED)

        val entries = hashSetOf<String>()

        val prefixes = hashMapOf<ArtifactDesc, String>()
        cfg.explicitPrefixedFroms.forEach { kv -> kv.getValue().collect(cfg.selector.scenarios).forEach { prefixes.put(it, kv.getKey()) } }

        fun getPrefix(artifact: ArtifactDesc) = prefixes.get(artifact) ?: ""

        for (sourcePair in src.filterIn(cfg.fromSources)) {
            val sourceDesc = sourcePair.first
            when (sourceDesc) {
                // note: order matters
                // \todo consider redesign dispatching so order is not important any more
                is FileGlobArtifact ->
                    if (sourceDesc.type == komplex.dsl.artifacts.jar) addFromJars(prefixes.get(sourceDesc), sourcePair, jarStream, cfg.deflate, entries)
                    else komplex.data.openFileSet(sourcePair).coll.forEach { add(prefixes.get(sourceDesc), sourceDesc.path, it.path, openInputStream(it), jarStream, cfg.deflate, entries) }
                is FolderArtifact -> komplex.data.openFileSet(sourcePair).coll.forEach { add(prefixes.get(sourceDesc), sourceDesc.path, it.path, openInputStream(it), jarStream, cfg.deflate, entries) }
                is FileArtifact ->
                    if (sourceDesc.type == komplex.dsl.artifacts.jar) addFromJars(prefixes.get(sourceDesc), sourcePair, jarStream, cfg.deflate, entries)
                    else add(prefixes.get(sourceDesc), sourceDesc.path.getParent(), sourceDesc.path, openInputStream(openFileSet(sourcePair).coll.single()), jarStream, cfg.deflate, entries)
                else -> throw IllegalArgumentException("$sourceDesc is not supported in $name")
            }
        }
        jarStream.close()
        log.info("$name succeeded")
        return BuildResult(BuildDiagnostic.Success, listOf(Pair(targetDesc, targetData)))
    }
}

