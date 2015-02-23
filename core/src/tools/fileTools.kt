
package komplex.tools

import komplex.model.BuildContext
import komplex.model.ArtifactDesc
import komplex.model.ArtifactData
import komplex.model.BuildResult
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import komplex.dsl.FileArtifact
import komplex.dsl.FolderArtifact
import komplex.data.openInputStream
import komplex.data.FileData
import komplex.data.InputStreamData
import komplex.data.SimpleFileData
import komplex.utils.BuildDiagnostic
import java.nio.file.Path

public val komplex.dsl.tools.copy: CopyToolRule get() = CopyToolRule()


public class CopyToolRule : komplex.dsl.BasicToolRule<CopyToolRule, CopyTool>(CopyTool()) {}


// copies all sources to all destinations
public class CopyTool : komplex.model.Tool<CopyToolRule> {
    override val name: String = "copy"

    override fun execute(context: BuildContext, cfg: CopyToolRule, src: Iterable<Pair<ArtifactDesc, ArtifactData?>>, tgt: Iterable<ArtifactDesc>): BuildResult {
        val result = arrayListOf<Pair<ArtifactDesc, ArtifactData>>()
        for (destination in tgt) {
            for (source in src) {
                val sourceData = source.second
                val sourceStream = openInputStream(sourceData!!) // \todo proper exception here
                when (destination) {
                    is FileArtifact -> {
                        Files.copy(sourceStream.inputStream, destination.path, StandardCopyOption.REPLACE_EXISTING)
                        result.add(Pair(source.first, SimpleFileData(destination.path)))
                    }
                    is FolderArtifact -> {
                        val path = if (sourceData is FileData) destination.path.resolve(sourceData.path) else destination.path
                        Files.copy(sourceStream.inputStream, path, StandardCopyOption.REPLACE_EXISTING)
                        result.add(Pair(source.first, SimpleFileData(path)))
                    }
                    else -> throw IllegalArgumentException("$destination is not supported in copy tool")
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