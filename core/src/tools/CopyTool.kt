package komplex

import java.nio.file.*

public val tools.copy: CopyTool
    get() = CopyTool()

public class CopyTool : Converter("Copy") {
    override fun convert(context: BuildContext, from: List<BuildEndPoint>, to: List<BuildEndPoint>): BuildResult {
        for (destination in to) {
            for (source in from.getAllStreams()) {
                when (destination) {
                // TODO: change to streams
                    is BuildStreamEndPoint -> Files.copy(source.inputStream, destination.path, StandardCopyOption.REPLACE_EXISTING)
                    is FolderEndPoint -> Files.copy(source.inputStream, destination.path.resolve(source.path), StandardCopyOption.REPLACE_EXISTING)
                    else -> throw IllegalArgumentException("$destination is not supported in copy tool")
                }
            }
        }
        return BuildResult.Success
    }
}