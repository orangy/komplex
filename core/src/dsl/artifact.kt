
package komplex.dsl

import komplex.model.ArtifactData
import java.nio.file.Path
import java.nio.file.Paths
import java.util.SortedSet
import komplex.utils.fileSystem
import komplex.utils.resolvePath
import java.io.File

public trait GenericSourceType {}

public trait Artifact : komplex.model.ArtifactDesc, GenericSourceType {
    val `type`: ArtifactType
}

public trait ArtifactType {
}

public class NamedArtifactType(val name: String) : ArtifactType {
    override fun toString(): String = "($name)"
}

public object artifacts {
    public val unspecified: ArtifactType = NamedArtifactType("?")
    public val sources: ArtifactType = NamedArtifactType("src")
    public val resources: ArtifactType = NamedArtifactType("res")
    public val binaries: ArtifactType = NamedArtifactType("bin")
    public val jar: ArtifactType = NamedArtifactType("jar")
    public val configs: ArtifactType = NamedArtifactType("cfg")
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

public fun ScriptContext.file(type: ArtifactType, path: Path): FileArtifact = SimpleFileArtifact(`type`, this.resolvePath(path))
public fun ScriptContext.file(type: ArtifactType, path: String): FileArtifact = SimpleFileArtifact(`type`, this.resolvePath(path))


public class SimpleFileArtifact(override val type: ArtifactType, ipath: Path) : FileArtifact {
    init { assert(ipath.toFile().isFile(), "Expecting a file at '$ipath'") }
    override var basePath: Path = ipath.getParent().toAbsolutePath().normalize()
    override var relPath = basePath.relativize(ipath.toAbsolutePath().normalize())
    override val name: String get() = "$`type` file ${path}"

    override fun equals(other: Any?): Boolean = path.equals((other as? SimpleFileArtifact)?.path)
    override fun hashCode(): Int = 1783 * path.hashCode()
}


public trait FileSetArtifact : PathBasedArtifact {}

public fun ScriptContext.folder(type: ArtifactType, path: Path): FolderArtifact =  FolderArtifact(`type`, this.resolvePath(path))
public fun ScriptContext.folder(type: ArtifactType, path: String): FolderArtifact = FolderArtifact(`type`, this.resolvePath(path))

public abstract class AbstractFolderBasedArtifact(override val type: ArtifactType, ipath: Path) : FileSetArtifact {
    init { assert(ipath.toFile().isDirectory(), "Expecting a directory at '$ipath'") }
    override var basePath: Path = ipath.toAbsolutePath().normalize()
    override var relPath = Paths.get(".")
}


public class FolderArtifact(type: ArtifactType, ipath: Path) : AbstractFolderBasedArtifact(type, ipath) {
    override val name: String = "$`type` folder ${path}"

    override fun equals(other: Any?): Boolean = path.equals((other as? FolderArtifact)?.path)
    override fun hashCode(): Int = 887 * path.hashCode()
}

public fun FolderArtifact.div(p: Path): Path = path.resolve(p)
public fun FolderArtifact.div(p: String): Path = path.resolve(p)

public class GlobCollection(val collection: MutableList<String>) {
    public fun path(value: String) {
        collection.add(value)
    }
}

public fun ScriptContext.files(type: ArtifactType): FileGlobArtifact = FileGlobArtifact(type, this.resolvePath("."))
public fun ScriptContext.files(type: ArtifactType, base: Path): FileGlobArtifact = FileGlobArtifact(`type`, this.resolvePath(base))
public fun ScriptContext.files(type: ArtifactType, base: String): FileGlobArtifact = FileGlobArtifact(`type`, this.resolvePath(base))
public fun ScriptContext.files(type: ArtifactType, base: PathBasedArtifact): FileGlobArtifact = FileGlobArtifact(`type`, this.resolvePath(base.path))
public fun ScriptContext.files(type: ArtifactType, base: Path, include: String): FileGlobArtifact = files(type, base).include(include)
public fun ScriptContext.files(type: ArtifactType, base: String, include: String): FileGlobArtifact = files(type, base).include(include)
public fun ScriptContext.files(type: ArtifactType, base: PathBasedArtifact, include: String): FileGlobArtifact = files(type,base).include(include)
// context independent variant with path as a base
public fun files(type: ArtifactType, base: Path, include: String): FileGlobArtifact =
        FileGlobArtifact(`type`, base).include(include)

public class FileGlobArtifact(type: ArtifactType, base: Path) : AbstractFolderBasedArtifact(type, base) {
    var included = arrayListOf<String>()
    var excluded = arrayListOf<String>()

    override fun equals(other: Any?): Boolean = super.equals(other) &&
            included.equals((other as? FileGlobArtifact)?.included) &&
            excluded.equals((other as? FileGlobArtifact)?.excluded)
    override fun hashCode(): Int = 1013 * path.hashCode() + 1009 * included.hashCode() + 239 * excluded.hashCode()

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

public class ArtifactsSet(public val members: Collection<Artifact>): GenericSourceType {
    override fun equals(other: Any?): Boolean = members.toHashSet().equals((other as? ArtifactsSet)?.members?.toHashSet())
    override fun hashCode(): Int = members.fold(0, { r, a -> r + 17 * a.hashCode() })
}

public fun ScriptContext.artifactsSet(vararg artifacts: Artifact) : ArtifactsSet = ArtifactsSet(artifacts.toArrayList())

public fun ScriptContext.artifactsSet(artifacts: Iterable<Any>): ArtifactsSet =
    ArtifactsSet(artifacts.flatMap { when (it) {
        is ArtifactsSet -> it.members
        is Artifact -> listOf(it)
        is Iterable<*> -> artifactsSet(it).members
        else -> throw Exception("Unknown argument for ArtifactsSet: $it")
    }})

public fun ScriptContext.artifactsSet(vararg artifacts: Any): ArtifactsSet = artifactsSet(artifacts.asIterable())



// for reference types
public class VariableArtifact<T: Any>(override val type: ArtifactType, public val ref: T) : Artifact {
    override val name: String = "$`type` var ${ref}"
    override fun equals(other: Any?): Boolean = ref.equals((other as? VariableArtifact<T>)?.ref)
    override fun hashCode(): Int = ref.hashCode()
}

public fun variable<T: Any>(type: ArtifactType, ref: T) : VariableArtifact<T> = VariableArtifact(type, ref)

