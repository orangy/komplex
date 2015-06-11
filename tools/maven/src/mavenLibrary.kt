package komplex.tools.maven

import komplex.dsl
import komplex.dsl.*
import komplex.model.ConditionalModuleDependency
import komplex.model.Module
import komplex.model.ModuleMetadata
import kotlin.properties.Delegates

public data class MavenLibraryArtifact(public val id: MavenId) : dsl.Artifact {
    override val type = dsl.artifacts.jar
    override val name: String get() = id.toString()

    public fun targetName(): String {
        return "${id.artifactId}${if (!id.version.isEmpty()) "-$id.version" else ""}${ext()}"
    }
    private fun ext() = ".jar" /* when (type) {
        artifacts.jar -> ".jar"
        else -> ""
    } */

    override fun equals(other: Any?): Boolean = targetName().equals((other as? MavenLibraryArtifact)?.targetName())
    override fun hashCode(): Int = 2539 * targetName().hashCode()
}

public data class MavenResolvedLibraryArtifact(val sourceArtifact: MavenLibraryArtifact) : dsl.Artifact {
    override val type: ArtifactType get() = sourceArtifact.type
    override val name: String get() = sourceArtifact.name

    override fun equals(other: Any?): Boolean = sourceArtifact.equals((other as? MavenResolvedLibraryArtifact)?.sourceArtifact)
    override fun hashCode(): Int = 3347 * sourceArtifact.hashCode()
}


public class MavenLibraryModule(public val library: MavenLibraryArtifact, public val target: dsl.FolderArtifact)
: komplex.model.Module, GenericSourceType {

    override val metadata = object : ModuleMetadata {}
    override val dependencies: Iterable<ConditionalModuleDependency> = listOf()
    override val parent: Module? = null
    override val children: Iterable<Module> = listOf()
    override val name: String = "library ${library.name}"

    override val steps by Delegates.lazy {
        val step = dsl.tools.maven
        step from library into target
        step.export = true
        listOf(step)
    }

//    override fun targets(scenarios: Scenarios): Iterable<ArtifactDesc> =
//            listOf(MavenResolvedLibraryArtifact(library))
}

public data class MavenId(val groupId: String, val artifactId: String, val version: String) {
    override fun toString(): String = "$groupId:$artifactId:$version"
}

private fun mavenId(id: String, version: String?): MavenId {
    val names = id.split("[/:]".toRegex())
    return MavenId(names[0],
            if (names.size() > 1) names[1] else names[0],
            version ?: if (names.size() > 2) names[2] else "")
}

public fun mavenLibrary(id: String, version: String?, target: FolderArtifact): MavenLibraryModule =
        MavenLibraryModule(MavenLibraryArtifact(mavenId(id, version)), target)
