package komplex

import java.io.File
import java.nio.file.Path

class RepositoryResolveResult(val classPath: String) {
}

open public class Repository(val project: Project) {

    fun resolve(reference: LibraryReference): RepositoryResolveResult? {
        val result = resolveReference(reference)
        if (result != null)
            return result
        val parentProject = project.parent
        if (parentProject != null) {
            return parentProject.repository.resolve(reference)
        }
        return null
    }

    fun resolveReference(reference: LibraryReference): RepositoryResolveResult? {
        val repo = findRepository()
        val jarName = when {
            reference.version.isEmpty() -> reference.name + ".jar"
            else -> reference.name + "-" + reference.version + ".jar"
        }

        val file = repo.resolve(jarName)?.toFile()
        if (file != null && file.exists())
            return RepositoryResolveResult(file.getAbsolutePath())
        return null
    }

    fun findRepository(): Path {
        var repoFolder: File? = File("").getAbsoluteFile()
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
        return repo
    }

    fun publish(endpoint: BuildEndPoint) {
    }

    fun dump(indent: String) {
        println("$indent Repository")
    }
}