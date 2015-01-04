package komplex

import java.nio.file.*

public val tools.copy: Converter.Rule
    get() = Converter.Rule(CopyTool())

public class CopyTool : Converter("Copy") {
    override fun convert(context: BuildStepContext, from: Iterable<Artifact>, to: Iterable<Artifact>): BuildResult {
        for (destination in to) {
            for (source in from.getAllStreams()) {
                when (destination) {
                // TODO: change to streams
                    is StreamArtifact -> Files.copy(source.inputStream, destination.path, StandardCopyOption.REPLACE_EXISTING)
                    is FolderArtifact -> Files.copy(source.inputStream, destination.path.resolve(source.path), StandardCopyOption.REPLACE_EXISTING)
                    else -> throw IllegalArgumentException("$destination is not supported in copy tool")
                }
            }
        }
        return BuildResult.Success
    }
}