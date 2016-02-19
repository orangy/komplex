
package komplex.tools

import komplex.data.SimpleFileData
import komplex.data.openFileSet
import komplex.data.openInputStream
import komplex.dsl.*
import komplex.log
import komplex.model.ArtifactData
import komplex.model.ArtifactDesc
import komplex.model.BuildContext
import komplex.model.BuildResult
import komplex.utils.BuildDiagnostic
import komplex.utils.plus
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

// ----------------------------------

@Suppress("unused")
val komplex.dsl.tools.copy: CopyToolRule get() = CopyToolRule()


class CopyToolRule : komplex.dsl.BasicToolRule<CopyToolRule, CopyTool>(CopyTool()) {

    override fun configure(): BuildDiagnostic {
        val guessedType = (sources.firstOrNull() as? Artifact?)?.type ?: artifacts.unspecified
        var res = super.configure()
        if (explicitTargets.none()) {
            if (module.env.defaultTargetDir != null) explicitTargets.add(module.folder(guessedType, module.env.defaultTargetDir!!))
            else res = res + BuildDiagnostic.Fail("$name (${module.fullName}) Cannot auto configure target folder: defaultTargetDir is not defined")
        }
        // if copying files to directories, replace each directory target with individual targets, so conflicts
        // could be avoided automatically (or detected if file names clash)
        if (explicitTargets.any() && sources.all { it is FileArtifact }) {
            val newTargets = arrayListOf<Artifact>()
            for (tgt in explicitTargets)
                when (tgt) {
                    is FolderArtifact ->
                        for (src in sources)
                            when (src) {
                                is FileArtifact -> newTargets.add(module.file(src.type, tgt.path.resolve(src.path.getFileName())))
                                else -> throw Exception("unexpected source for copy: $src") // \todo - find out more valid cases
                            }
                    else -> newTargets.add(tgt)
                }
            explicitTargets.clear()
            explicitTargets.addAll(newTargets)
            // effectively noop, see comment above
            res = res + BuildDiagnostic.Success
        }
        return res
    }

    var makeDirs: Boolean = true
}


// copies all sources to all destinations
class CopyTool : komplex.model.Tool<CopyToolRule> {
    override val name: String = "copy"

    override fun execute(context: BuildContext, cfg: CopyToolRule, src: Iterable<Pair<ArtifactDesc, ArtifactData?>>, tgt: Iterable<ArtifactDesc>): BuildResult {
        val result = arrayListOf<Pair<ArtifactDesc, ArtifactData>>()
        for (destination in tgt) {
            log.debug("copying into target $destination")
            for (source in src) {
                log.debug("copying from source ${source.first}")
                val sourceFiles = openFileSet(source)
                when (destination) {
                    is FileArtifact -> {
                        if (cfg.makeDirs && !destination.path.getParent().toFile().exists()) destination.path.getParent().toFile().mkdirs()
                        sourceFiles.coll.forEach {
                            Files.copy(openInputStream(it).inputStream, destination.path, StandardCopyOption.REPLACE_EXISTING)
                            result.add(Pair(source.first, SimpleFileData(destination.path)))
                        }
                    }
                    is FolderArtifact -> {
                        if (cfg.makeDirs && !destination.path.toFile().exists()) destination.path.toFile().mkdirs()
                        sourceFiles.coll.forEach {
                            val path = destination.path.resolve(it.path)
                            Files.copy(openInputStream(it).inputStream, path, StandardCopyOption.REPLACE_EXISTING)
                            result.add(Pair(source.first, SimpleFileData(path)))
                        }
                    }
                    else -> throw IllegalArgumentException("$destination is not supported as a destination in copy tool")
                }
            }
        }
        return BuildResult( BuildDiagnostic.Success, result)
    }
}

// ----------------------------------

@Suppress("unused")
val komplex.dsl.tools.find: FindInPathsToolRule get() = FindInPathsToolRule()


class FindInPathsToolRule : komplex.dsl.BasicToolRule<FindInPathsToolRule, FindInPathsTool>(FindInPathsTool()) {
    val paths: MutableList<Path> = arrayListOf()
}

// finds all sources in preconfigured paths, ignores targets - add sources as a key in destinations
class FindInPathsTool : komplex.model.Tool<FindInPathsToolRule> {
    override val name: String = "find in paths"

    override fun execute(context: BuildContext, cfg: FindInPathsToolRule, src: Iterable<Pair<ArtifactDesc, ArtifactData?>>, tgt: Iterable<ArtifactDesc>): BuildResult {
        val result = arrayListOf<Pair<ArtifactDesc, ArtifactData>>()
        for (source in src) {
            val sourceDesc = source.first
            val res: ArtifactData? =
                    when (sourceDesc) {
                        is FileArtifact -> {
                            val foundPath = cfg.paths.map { sourceDesc.path.resolve(it) }.first { Files.exists(it) }
                            if (foundPath != null) SimpleFileData(foundPath) else null
                        }
                        // \todo add support for glob
                        else -> throw UnsupportedOperationException("Do not know how to search for '$sourceDesc'")
                    }
            if (res != null) result.add(Pair(sourceDesc, res))
        }
        return BuildResult( if (result.isEmpty()) BuildDiagnostic.Fail else BuildDiagnostic.Success, result)
    }
}

// ----------------------------------

val komplex.dsl.tools.echo: EchoToolRule get() = EchoToolRule()


class EchoToolRule : komplex.dsl.BasicToolRule<EchoToolRule, EchoTool>(EchoTool()) {
    // \todo make it an (string) artifact, so checksum could be calculated
    var sourceStr: String = ""
    var makeDirs: Boolean = true
    var charset: String = "UTF-8"
}


// echoes a string to all destinations
class EchoTool : komplex.model.Tool<EchoToolRule> {
    override val name: String = "echo"

    override fun execute(context: BuildContext, cfg: EchoToolRule, src: Iterable<Pair<ArtifactDesc, ArtifactData?>>, tgt: Iterable<ArtifactDesc>): BuildResult {
        val result = arrayListOf<Pair<ArtifactDesc, ArtifactData>>()
        for (destination in tgt) {
            log.debug("echoing \"${cfg.sourceStr}\" into target $destination")
            if (destination is FileArtifact) {
                if (cfg.makeDirs && !destination.path.getParent().toFile().exists()) destination.path.getParent().toFile().mkdirs()
                destination.path.toFile().writeText(cfg.sourceStr, Charset.forName(cfg.charset))
                result.add(Pair(destination, SimpleFileData(destination.path)))
            }
            else throw IllegalArgumentException("$destination is not supported as a destination in echo tool")
        }
        return BuildResult( BuildDiagnostic.Success, result)
    }
}

infix fun EchoToolRule.from(str: String): EchoToolRule {
    sourceStr = str
    return this
}
