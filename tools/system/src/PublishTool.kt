package komplex.system

import java.nio.file.*
import komplex.*
import java.io.File

val tools.publish = PublishTool()
class PublishTool : Tool("Publish") {
    override fun execute(process: BuildProcess, from: List<BuildEndPoint>, to: List<BuildEndPoint>): BuildResult {
        var repoFolder : File? = File("").getAbsoluteFile()
        while (repoFolder != null) {
            val current = repoFolder!!
            if (current.isDirectory() && current.listFiles { it.name == ".repository" }?.any() == true) {
                break
            }
            repoFolder = repoFolder?.getParentFile()
        }

        val repo = repoFolder?.toPath()?.resolve(".repository")
        if (repo == null)
            throw IllegalStateException("None of parent folders contains .repository folder")

        for (source in from.getAllStreams()) {
            Files.copy(source.inputStream, repo.resolve(source.path.getFileName()!!), StandardCopyOption.REPLACE_EXISTING)
        }
        return BuildResult.Success
    }
}