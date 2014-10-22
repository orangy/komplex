package komplex

import java.nio.file.*

public val tools.publish: PublishTool
    get() = PublishTool()

public class PublishTool : Consumer("Publish") {
    public override fun consume(context: BuildStep, from: List<Artifact>): BuildResult {
/*
        val repo = context.module.repository.findRepository()

        for (source in from.getAllStreams()) {
            Files.copy(source.inputStream, repo.resolve(source.path.getFileName()!!), StandardCopyOption.REPLACE_EXISTING)
        }
*/
        return BuildResult.Success
    }
}