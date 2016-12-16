
package komplex.dsl

import komplex.utils.resolvePath
import java.nio.file.Path
import java.nio.file.Paths

interface GenericSourceType {}

interface Artifact : komplex.model.ArtifactDesc, GenericSourceType {
    val `type`: ArtifactType
}

interface ArtifactType {
}

class NamedArtifactType(val name: String, val extension: String? = null) : ArtifactType {
    override fun toString(): String = "($name)"
    fun updateExtension(p: Path): Path {
        // \todo think about asserting the extension here
        if (extension == null) return p
        val ext = if (extension.startsWith(".")) extension else ".$extension"
        return if (p.fileName.toString().endsWith(ext, ignoreCase = true)) p else p.parent.resolve(p.fileName.toString() + ext)
    }
}


object artifacts {
    val unspecified: ArtifactType = NamedArtifactType("?")
    val source: ArtifactType = NamedArtifactType("src")
    fun source(extension: String): ArtifactType = NamedArtifactType("src", extension)
    val resource: ArtifactType = NamedArtifactType("res")
    fun resource(extension: String): NamedArtifactType = NamedArtifactType("res", extension)
    val binary: ArtifactType = NamedArtifactType("bin")
    fun binary(extension: String): NamedArtifactType = NamedArtifactType("bin", extension)
    val jar: ArtifactType = NamedArtifactType("jar", "jar")
    fun jar(extension: String): NamedArtifactType = NamedArtifactType("jar", extension)
    val config: ArtifactType = NamedArtifactType("cfg")
    fun config(extension: String): NamedArtifactType = NamedArtifactType("cfg", extension)
}

interface PathBasedArtifact : Artifact {
    val basePath: Path
    val relPath: Path
    val path: Path get() = basePath.resolve(relPath).normalize()
}


interface FileArtifact : PathBasedArtifact { }

class SimpleFileArtifact(override val type: ArtifactType, ipath: Path) : FileArtifact {
    init { assert(ipath.toFile().isFile, { "Expecting a file at '$ipath'" }) }
    override val basePath: Path = ipath.parent.toAbsolutePath().normalize()
    override val relPath = basePath.relativize((if (type is NamedArtifactType) type.updateExtension(ipath) else ipath).toAbsolutePath().normalize())
    override val name: String get() = "$`type` file ${path}"

    override fun equals(other: Any?): Boolean = path == (other as? SimpleFileArtifact)?.path
    override fun hashCode(): Int = 1783 * path.hashCode()
}


interface FileSetArtifact : PathBasedArtifact {}

abstract class AbstractFolderBasedArtifact(override val type: ArtifactType, ipath: Path) : FileSetArtifact {
    init { assert(ipath.toFile().isDirectory, { "Expecting a directory at '$ipath'" }) }
    override val basePath: Path = ipath.toAbsolutePath().normalize()
    override val relPath = Paths.get(".")
}


class FolderArtifact(type: ArtifactType, ipath: Path) : AbstractFolderBasedArtifact(type, ipath) {
    override val name: String = "$`type` folder $path"

    override fun equals(other: Any?): Boolean = path == (other as? FolderArtifact)?.path
    override fun hashCode(): Int = 887 * path.hashCode()
}


class GlobCollection(val collection: MutableList<String>) {
    fun path(value: String) {
        collection.add(value)
    }
}


class FileGlobArtifact(type: ArtifactType,
                              base: Path,
                              val included: Iterable<String>,
                              val excluded: Iterable<String>)
: AbstractFolderBasedArtifact(type, base) {

    override fun equals(other: Any?): Boolean = super.equals(other) &&
            included == (other as? FileGlobArtifact)?.included &&
            excluded == (other as? FileGlobArtifact)?.excluded
    override fun hashCode(): Int = 1013 * path.hashCode() + 1009 * included.hashCode() + 239 * excluded.hashCode()

    fun invoke(body: FileGlobArtifact.() -> Unit) {
        this.body()
    }

    override val name: String get() = "$`type` folder $path glob +$included -$excluded"
}


class ArtifactsSet(val members: Collection<Artifact>): GenericSourceType {
    override fun equals(other: Any?): Boolean = members.toHashSet() == (other as? ArtifactsSet)?.members?.toHashSet()
    override fun hashCode(): Int = members.fold(0, { r, a -> r + 17 * a.hashCode() })
}


fun ScriptContext.artifactsSet(artifacts: Iterable<Any>): ArtifactsSet =
    ArtifactsSet(artifacts.flatMap { when (it) {
        is ArtifactsSet -> it.members
        is Artifact -> listOf(it)
        is Iterable<*> -> artifactsSet(it).members
        else -> throw Exception("Unknown argument for ArtifactsSet: $it")
    }})


// for reference types
class VariableArtifact<T: Any>(override val type: ArtifactType, val ref: T) : Artifact {
    override val name: String = "$`type` var ${ref}"
    override fun equals(other: Any?): Boolean = ref == (other as? VariableArtifact<T>)?.ref
    override fun hashCode(): Int = ref.hashCode()
}

