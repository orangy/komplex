
package komplex.dsl

import komplex.utils.resolvePath
import java.nio.file.Path
import java.nio.file.Paths

public interface GenericSourceType {}

public interface Artifact : komplex.model.ArtifactDesc, GenericSourceType {
    val `type`: ArtifactType
}

public interface ArtifactType {
}

public open class NamedArtifactType(val name: String) : ArtifactType {
    override fun toString(): String = "($name)"
}

public class NamedArtifactTypeWithDefaultExtension(name: String, val ext: String) : NamedArtifactType(name) {
    // \todo write correct implementation handling corner cases
    fun updateExtension(p: Path): Path = if (p.getFileName().toString().endsWith(ext)) p else p.getParent().resolve(p.getFileName().toString() + ext)
}


public object artifacts {
    public val unspecified: ArtifactType = NamedArtifactType("?")
    public val sources: ArtifactType = NamedArtifactType("src")
    public val resources: ArtifactType = NamedArtifactType("res")
    public val binaries: ArtifactType = NamedArtifactType("bin")
    public val jar: ArtifactType = NamedArtifactTypeWithDefaultExtension("jar", ".jar")
    public val configs: ArtifactType = NamedArtifactType("cfg")
}

public interface PathBasedArtifact : Artifact {
    val basePath: Path
    val relPath: Path
    public val path: Path get() = basePath.resolve(relPath).normalize()
}


public interface FileArtifact : PathBasedArtifact { }

public class SimpleFileArtifact(override val type: ArtifactType, ipath: Path) : FileArtifact {
    init { assert(ipath.toFile().isFile(), "Expecting a file at '$ipath'") }
    override val basePath: Path = ipath.getParent().toAbsolutePath().normalize()
    override val relPath = basePath.relativize((if (type is NamedArtifactTypeWithDefaultExtension) type.updateExtension(ipath) else ipath).toAbsolutePath().normalize())
    override val name: String get() = "$`type` file ${path}"

    override fun equals(other: Any?): Boolean = path.equals((other as? SimpleFileArtifact)?.path)
    override fun hashCode(): Int = 1783 * path.hashCode()
}


public interface FileSetArtifact : PathBasedArtifact {}

public abstract class AbstractFolderBasedArtifact(override val type: ArtifactType, ipath: Path) : FileSetArtifact {
    init { assert(ipath.toFile().isDirectory(), "Expecting a directory at '$ipath'") }
    override val basePath: Path = ipath.toAbsolutePath().normalize()
    override val relPath = Paths.get(".")
}


public class FolderArtifact(type: ArtifactType, ipath: Path) : AbstractFolderBasedArtifact(type, ipath) {
    override val name: String = "$`type` folder ${path}"

    override fun equals(other: Any?): Boolean = path.equals((other as? FolderArtifact)?.path)
    override fun hashCode(): Int = 887 * path.hashCode()
}


public class GlobCollection(val collection: MutableList<String>) {
    public fun path(value: String) {
        collection.add(value)
    }
}


public class FileGlobArtifact(type: ArtifactType,
                              base: Path,
                              public val included: Iterable<String>,
                              public val excluded: Iterable<String>)
: AbstractFolderBasedArtifact(type, base) {

    override fun equals(other: Any?): Boolean = super.equals(other) &&
            included.equals((other as? FileGlobArtifact)?.included) &&
            excluded.equals((other as? FileGlobArtifact)?.excluded)
    override fun hashCode(): Int = 1013 * path.hashCode() + 1009 * included.hashCode() + 239 * excluded.hashCode()

    public fun invoke(body: FileGlobArtifact.() -> Unit) {
        this.body()
    }

    override val name: String get() = "$`type` folder $path glob +$included -$excluded"
}


public class ArtifactsSet(public val members: Collection<Artifact>): GenericSourceType {
    override fun equals(other: Any?): Boolean = members.toHashSet().equals((other as? ArtifactsSet)?.members?.toHashSet())
    override fun hashCode(): Int = members.fold(0, { r, a -> r + 17 * a.hashCode() })
}


public fun ScriptContext.artifactsSet(artifacts: Iterable<Any>): ArtifactsSet =
    ArtifactsSet(artifacts.flatMap { when (it) {
        is ArtifactsSet -> it.members
        is Artifact -> listOf(it)
        is Iterable<*> -> artifactsSet(it).members
        else -> throw Exception("Unknown argument for ArtifactsSet: $it")
    }})


// for reference types
public class VariableArtifact<T: Any>(override val type: ArtifactType, public val ref: T) : Artifact {
    override val name: String = "$`type` var ${ref}"
    override fun equals(other: Any?): Boolean = ref.equals((other as? VariableArtifact<T>)?.ref)
    override fun hashCode(): Int = ref.hashCode()
}

