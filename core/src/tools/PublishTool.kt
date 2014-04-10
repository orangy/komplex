package komplex

import java.nio.file.*
import java.io.File

val tools.publish = PublishTool()
class PublishTool : Tool("Publish") {
    override fun execute(context: BuildContext, from: List<BuildEndPoint>, to: List<BuildEndPoint>): BuildResult {
        val repo = context.project.repository.findRepository()

        for (source in from.getAllStreams()) {
            Files.copy(source.inputStream, repo.resolve(source.path.getFileName()!!), StandardCopyOption.REPLACE_EXISTING)
        }
        return BuildResult.Success
    }
}