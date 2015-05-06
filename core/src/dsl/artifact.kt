
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

public trait PathBasedArtifact : Artifact {
    // storing path in two parts, base and relative, allowing to rebase with base setter
    var basePath: Path
    var relPath: Path
    public val path: Path get() = basePath.resolve(relPath).normalize()
    public var base: Path
        get() = basePath
        set(v: Path) {
            val newBase = v.toAbsolutePath().normalize()
            relPath = newBase.relativize(path)
            basePath = newBase
        }
}

public fun<A: PathBasedArtifact> A.base(p: Path): A { this.base = p.toAbsolutePath().normalize(); return this }
public fun<A: PathBasedArtifact> A.base(p: String): A = this.base(Paths.get(p))

public trait FileArtifact : PathBasedArtifact { }

public fun file(type: ArtifactType, path: Path): FileArtifact = SimpleFileArtifact(`type`, path)
public fun file(type: ArtifactType, path: String): FileArtifact = SimpleFileArtifact(`type`, fileSystem.getPath(path))

public class SimpleFileArtifact(override val type: ArtifactType, ipath: Path) : FileArtifact {
    init { assert(ipath.toFile().isFile(), "Expecting a file at '$ipath'") }
    override var basePath: Path = ipath.getParent().toAbsolutePath().normalize()
    override var relPath = basePath.relativize(ipath.toAbsolutePath().normalize())
    override val name: String = "$`type` file ${path}"
}

public trait FileSetArtifact : PathBasedArtifact {}

public fun folder(type: ArtifactType, path: Path): FolderArtifact = FolderArtifact(`type`, path)
public fun folder(type: ArtifactType, path: String): FolderArtifact = FolderArtifact(`type`, fileSystem.getPath(path))

public open class FolderArtifact(override val type: ArtifactType, ipath: Path) : FileSetArtifact {
    init { assert(ipath.toFile().isDirectory(), "Expecting a directory at '$ipath'") }
    override var basePath: Path = ipath.toAbsolutePath().normalize()
    override var relPath = Paths.get(".")
    override val name: String = "$`type` folder ${path}"
}

public class GlobCollection(val collection: MutableList<String>) {
    public fun path(value: String) {
        collection.add(value)
    }
}

public fun files(type: ArtifactType): FileGlobArtifact = FileGlobArtifact(type, Paths.get("."))
public fun files(type: ArtifactType, include: String): FileGlobArtifact = files(type).include(include)
public fun files(type: ArtifactType, base: Path, include: String): FileGlobArtifact = FileGlobArtifact(`type`, base).include(include)

open class FileGlobArtifact(type: ArtifactType, base: Path) : FolderArtifact(type, base) {
    var included = arrayListOf<String>()
    var excluded = arrayListOf<String>()

    public fun invoke(body: FileGlobArtifact.() -> Unit) {
        this.body()
    }

    public fun append(files: FileGlobArtifact): FileGlobArtifact {
        included.addAll(files.included)
        excluded.addAll(files.excluded)
        return this
    }

    public fun include(vararg glob: String): FileGlobArtifact {
        included.addAll(glob)
        return this
    }

    public fun all(): FileGlobArtifact {
        included.add("**")
        return this
    }

    public fun exclude(vararg glob: String): FileGlobArtifact {
        excluded.addAll(glob)
        return this
    }

    public fun include(body: GlobCollection.() -> Unit): FileGlobArtifact {
        val collection = GlobCollection(included)
        collection.body()
        return this
    }

    public fun exclude(body: GlobCollection.() -> Unit): FileGlobArtifact {
        val collection = GlobCollection(excluded)
        collection.body()
        return this
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
