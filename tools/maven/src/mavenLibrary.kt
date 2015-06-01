package komplex.tools.maven

import java.nio.file.Path
import komplex.dsl
import komplex.dsl.ArtifactType
import kotlin.properties.Delegates
import komplex.dsl.from
import komplex.dsl.FolderArtifact
import komplex.dsl.into
import komplex.model.ArtifactDesc
import komplex.model.Scenarios

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
: komplex.dsl.Module(null, "library ${library.name}") {
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
