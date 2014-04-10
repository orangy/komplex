package komplex.system

import java.nio.file.*
import komplex.*

val tools.copy = CopyTool()
class CopyTool : Tool("Copy") {
    override fun execute(from: List<BuildEndPoint>, to: List<BuildEndPoint>) {
        for (destination in to) {
            for (source in from.getAllStreams()) {
                when (destination) {
                    // TODO: change to streams
                    is BuildStreamEndPoint -> Files.copy(source.inputStream, destination.path, StandardCopyOption.REPLACE_EXISTING)
                    is BuildFolder -> Files.copy(source.inputStream, destination.path, StandardCopyOption.REPLACE_EXISTING)
                    else -> throw IllegalArgumentException("$destination is not supported in copy tool")
                }
            }
        }
    }
}