package komplex

import java.nio.file.*

public val tools.publish: PublishTool
    get() = PublishTool()

public class PublishTool : ConsumingTool("Publish") {
    public override fun execute(context: BuildContext, from: List<BuildEndPoint>): BuildResult {
/*
        val repo = context.module.repository.findRepository()

        for (source in from.getAllStreams()) {
            Files.copy(source.inputStream, repo.resolve(source.path.getFileName()!!), StandardCopyOption.REPLACE_EXISTING)
        }
*/
        return BuildResult.Success
    }
}