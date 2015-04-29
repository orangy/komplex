
package komplex.dsl

import komplex.model.ArtifactData
import java.nio.file.Path
import java.nio.file.Paths
import java.util.SortedSet
import komplex.utils.fileSystem
import java.io.File

public trait Artifact : komplex.model.ArtifactDesc {
    val `type`: ArtifactType
}

public trait ArtifactType {
}

public class NamedArtifactType(val name: String) : ArtifactType {
    override fun toString(): String = "($name)"
}

public object artifacts {
    public val sources: ArtifactType = NamedArtifactType("src")
    public val binaries: ArtifactType = NamedArtifactType("bin")
    public val jar: ArtifactType = NamedArtifactType("jar")
}

public trait FileArtifact : Artifact{
    public val path: Path
}

public fun file(path: Path, `type`: ArtifactType): FileArtifact = SimpleFileArtifact(path, `type`)
public fun file(path: String, `type`: ArtifactType): FileArtifact = SimpleFileArtifact(fileSystem.getPath(path), `type`)
public class SimpleFileArtifact(override val path: Path, override val `type`: ArtifactType) : FileArtifact {
    override val name: String = "$`type` file ${path}"
}

public trait FileSetArtifact : Artifact {}

public fun folder(path: Path, `type`: ArtifactType): FolderArtifact = FolderArtifact(path, `type`)
public fun folder(path: String, `type`: ArtifactType): FolderArtifact = FolderArtifact(fileSystem.getPath(path), `type`)

public open class FolderArtifact(public val path: Path, override val `type`: ArtifactType) : FileSetArtifact {
    override val name: String = "$`type` folder ${path}"
}

public class GlobCollection(val collection: MutableList<String>) {
    public fun path(value: String) {
        collection.add(value)
    }
}

public fun files(glob: String, `type`: ArtifactType, base: String? = null): Artifact =
        FileGlobArtifact(if (base != null) Paths.get(base) else Paths.get(glob).getParent(),`type`).let { it.include(glob); it }

public fun files(glob: String, `type`: ArtifactType, base: Path): Artifact =
        FileGlobArtifact(base,`type`).let { it.include(glob); it }

class FileGlobArtifact(base: Path, type: ArtifactType) : FolderArtifact(base, type) {
    val included = arrayListOf<String>()
    val excluded = arrayListOf<String>()

    public fun invoke(body: FileGlobArtifact.() -> Unit) {
        this.body()
    }

    public fun append(files: FileGlobArtifact) {
        included.addAll(files.included)
        excluded.addAll(files.excluded)
    }

    public fun include(glob: String) {
        included.add(glob)
    }

    public fun exclude(glob: String) {
        excluded.add(glob)
    }

    public fun include(body: GlobCollection.() -> Unit) {
        val collection = GlobCollection(included)
        collection.body()
    }

    public fun exclude(body: GlobCollection.() -> Unit) {
        val collection = GlobCollection(excluded)
        collection.body()
    }

    override val name: String get() = "$`type` folder $path glob +$included -$excluded"
}

public open class ArtifactsSet(public val members: Collection<Artifact>) {}

public fun artifactsSet(vararg artifacts: Artifact) : ArtifactsSet = ArtifactsSet(artifacts.toArrayList())

public fun artifactsSet(artifacts: Iterable<Any>): ArtifactsSet =
    ArtifactsSet(artifacts.flatMap { when (it) {
        is ArtifactsSet -> it.members
        is Artifact -> listOf(it)
        is Iterable<*> -> artifactsSet(it).members
        else -> throw Exception("Unknown argument for ArtifactsSet: $it")
    }})

public fun artifactsSet(vararg artifacts: Any): ArtifactsSet = artifactsSet(artifacts.asIterable())
