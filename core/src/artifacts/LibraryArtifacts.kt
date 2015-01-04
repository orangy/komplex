
package komplex

import java.nio.file.Path
import java.util.ArrayList
import java.nio.file.Paths
import java.util.HashSet
import java.util.SortedSet

// reference (source) artifacts

public class LibraryReferenceArtifact(public val id: String, public val version: String?,
                             override val type: ArtifactType = artifacts.jar): Artifact {
    public fun targetName(): String {
        val mid = mavenId()
        return "${mid.artifactId}${if (mid.version != null && !mid.version.isEmpty()) "-$version" else ""}${ext()}"
    }
    private fun ext() = when (type) {
        artifacts.jar -> ".jar"
        else -> ""
    }
    public data class MavenId(val groupId: String, val artifactId: String, val version: String) {
        override fun toString(): String = "$groupId:$artifactId:$version"
    }
    public fun mavenId(): MavenId {
        val names = id.split("[/:]")
        return MavenId(names[0],
                       if (names.size() > 1) names[1] else names[0],
                       version ?: if (names.size() > 2) names[2] else "")
    }
}


// target library artifacts

public trait LibraryArtifact : Artifact {}

// \todo add LibraryInPaths or something to represent not yet found local library

public class SingleLibraryArtifact(public val path: Path, public override val `type`: ArtifactType = artifacts.jar) : LibraryArtifact {
}

public fun SingleLibraryArtifact(ref: LibraryReferenceArtifact, basePath: String = ".") : SingleLibraryArtifact
        = SingleLibraryArtifact(Paths.get(basePath, ref.targetName()), ref.type)


public class LibraryWithDependenciesArtifact(public val id: String, public override val `type`: ArtifactType = artifacts.jar): Artifact {
    var resolvedPathsVar: SortedSet<Path> = sortedSetOf()

    public var resolvedPaths: Iterable<Path> by object {
        fun get(self: LibraryWithDependenciesArtifact, propertyMetadata: PropertyMetadata): Iterable<Path> =
                self.resolvedPathsVar ?: arrayListOf()

        fun set(self: LibraryWithDependenciesArtifact, propertyMetadata: PropertyMetadata, newval: Iterable<Path>) {
            self.resolvedPathsVar = newval.toSortedSet()
        }
    }
}

