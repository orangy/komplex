
package komplex.tools

import komplex.data.*
import komplex.model.BuildContext
import komplex.model.ArtifactDesc
import komplex.model.ArtifactData
import komplex.model.BuildResult
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import komplex.dsl.FileArtifact
import komplex.dsl.FolderArtifact
import komplex.log
import komplex.utils.BuildDiagnostic
import java.nio.file.Path

public val komplex.dsl.tools.copy: CopyToolRule get() = CopyToolRule()


public class CopyToolRule : komplex.dsl.BasicToolRule<CopyToolRule, CopyTool>(CopyTool()) {
    public var makeDirs: Boolean = false
}


// copies all sources to all destinations
public class CopyTool : komplex.model.Tool<CopyToolRule> {
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


public val komplex.dsl.tools.find: FindInPathsToolRule get() = FindInPathsToolRule()


public class FindInPathsToolRule : komplex.dsl.BasicToolRule<FindInPathsToolRule, FindInPathsTool>(FindInPathsTool()) {
    public val paths: MutableList<Path> = arrayListOf()
}

// finds all sources in preconfigured paths, ignores targets - add sources as a key in destinations
public class FindInPathsTool : komplex.model.Tool<FindInPathsToolRule> {
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