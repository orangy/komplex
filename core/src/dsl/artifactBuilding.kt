@file:Suppress("unused")

package komplex.dsl

import komplex.utils.resolvePath
import java.nio.file.Path
import java.nio.file.Paths

// generic path manipulation
operator fun Path.div(p: Path): Path = resolve(p)
operator fun Path.div(p: String): Path = resolve(p)

// set of functions and operators to simplify artifacts description
// examples
// file % "path" / "abc".jar
// folder.bin % "path"
// files.jar % "path" / "*.jar"


// represent a particular Artifact kind being built (captured as T) when type is not specified yet
// separate type for beginning of the artifact expression, allowing only % operation on it
// context is needed for subsequent path resolving from a root folder
class PathBasedArtifactRoot<T: PathBasedArtifact>(val ctx: ScriptContext) {}


// represent a particular Artifact kind being built (captured as T) when type is specified right after the PathBasedArtifactRoot
// separate type created from PathBasedArtifactRoot, allowing only % operation on it
// context is needed for subsequent path resolving from a root folder
class TypedPathBasedArtifactRoot<T: PathBasedArtifact>(val ctx: ScriptContext, val type: ArtifactType) {}


// represent a particular Artifact kind being built (captured as T) when type is not specified yet
// separate type for middle of the artifact expression, allowing only / operation
class IncompletePathBasedArtifact<T: PathBasedArtifact>(val path: Path) {}


// represent typed ending of the artifact expression, passing it to the right argument of / operator yelds a ready artifact
class PathBasedArtifactTypedPart(val pathPart: Path, val type: ArtifactType) {}


// generic path-based artifact functions
// from root - create incomplete artifact by adding path/string/existing artifact's path to root
operator fun<T: PathBasedArtifact> PathBasedArtifactRoot<T>.mod(p: Path): IncompletePathBasedArtifact<T> = IncompletePathBasedArtifact(ctx.resolvePath(p))
operator fun<T: PathBasedArtifact> PathBasedArtifactRoot<T>.mod(p: String): IncompletePathBasedArtifact<T> = IncompletePathBasedArtifact(ctx.resolvePath(p))
operator fun<T: PathBasedArtifact> PathBasedArtifactRoot<T>.mod(p: PathBasedArtifact): IncompletePathBasedArtifact<T> = IncompletePathBasedArtifact(ctx.resolvePath(p.path))

// from typed root - not generic, see definitions for every artifact type
// \todo seek for generic solution

// from incomplete artifact - create incomplete artifact by adding path/string/existing artifact's path to base incomplete artifact
operator fun<T: PathBasedArtifact> IncompletePathBasedArtifact<T>.div(p: Path): IncompletePathBasedArtifact<T> = IncompletePathBasedArtifact(path.resolve(p))
operator fun<T: PathBasedArtifact> IncompletePathBasedArtifact<T>.div(p: String): IncompletePathBasedArtifact<T> = IncompletePathBasedArtifact(path.resolve(p))
operator fun<T: PathBasedArtifact> IncompletePathBasedArtifact<T>.div(p: PathBasedArtifact): IncompletePathBasedArtifact<T> = IncompletePathBasedArtifact(path.resolve(p.path))


// complete file artifact building functions
// from type + path/string, based on context
fun ScriptContext.file(type: ArtifactType, path: Path): FileArtifact = SimpleFileArtifact(type, resolvePath(path))
fun ScriptContext.file(type: ArtifactType, path: String): FileArtifact = SimpleFileArtifact(type, resolvePath(path))
// from typed root
operator fun TypedPathBasedArtifactRoot<FileArtifact>.mod(p: Path): FileArtifact = SimpleFileArtifact(type, ctx.resolvePath(p))
operator fun TypedPathBasedArtifactRoot<FileArtifact>.mod(p: String): FileArtifact = SimpleFileArtifact(type, ctx.resolvePath(p))
operator fun TypedPathBasedArtifactRoot<FileArtifact>.mod(p: PathBasedArtifact): FileArtifact = SimpleFileArtifact(type, ctx.resolvePath(p.path))
// from other file artifact
operator fun FileArtifact.div(p: Path): FileArtifact = SimpleFileArtifact(type, path.resolve(p))
operator fun FileArtifact.div(p: String): FileArtifact = SimpleFileArtifact(type, path.resolve(p))


// incomplete file artifact building functions
// from type - typed root
fun ScriptContext.file(type: ArtifactType): TypedPathBasedArtifactRoot<FileArtifact> = TypedPathBasedArtifactRoot(this, type)
// 0-ary - root
fun ScriptContext.file(): PathBasedArtifactRoot<FileArtifact> = PathBasedArtifactRoot(this)
val ScriptContext.file: PathBasedArtifactRoot<FileArtifact> get() = file()

// completing incomplete file artifact (or root) with type
operator fun IncompletePathBasedArtifact<FileArtifact>.div(pt: PathBasedArtifactTypedPart): FileArtifact = SimpleFileArtifact(pt.type, path.resolve(pt.pathPart))
operator fun PathBasedArtifactRoot<FileArtifact>.mod(pt: PathBasedArtifactTypedPart): FileArtifact = SimpleFileArtifact(pt.type, ctx.resolvePath(pt.pathPart))


// \todo add extrensions handling for folder and files artifacts

// complete folder artifact building functions
// from type + path/string, based on context
fun ScriptContext.folder(type: ArtifactType, path: Path): FolderArtifact =  FolderArtifact(type, resolvePath(path))
fun ScriptContext.folder(type: ArtifactType, path: String): FolderArtifact = FolderArtifact(type, resolvePath(path))
// from typed root
operator fun TypedPathBasedArtifactRoot<FolderArtifact>.mod(p: Path): FolderArtifact = FolderArtifact(type, ctx.resolvePath(p))
operator fun TypedPathBasedArtifactRoot<FolderArtifact>.mod(p: String): FolderArtifact = FolderArtifact(type, ctx.resolvePath(p))
operator fun TypedPathBasedArtifactRoot<FolderArtifact>.mod(p: PathBasedArtifact): FolderArtifact = FolderArtifact(type, ctx.resolvePath(p.path))
// from other folder artifact
operator fun FolderArtifact.div(p: Path): FolderArtifact = FolderArtifact(type, path.resolve(p))
operator fun FolderArtifact.div(p: String): FolderArtifact = FolderArtifact(type, path.resolve(p))


// incomplete folder artifact building functions
// from type - typed root
fun ScriptContext.folder(type: ArtifactType): TypedPathBasedArtifactRoot<FolderArtifact> = TypedPathBasedArtifactRoot(this, type)
// file artifact root creation ( *folder % "path" / "abc".jar )
fun ScriptContext.folder(): PathBasedArtifactRoot<FolderArtifact> = PathBasedArtifactRoot(this)
val ScriptContext.folder: PathBasedArtifactRoot<FolderArtifact> get() = folder()

// completing incomplete file artifact (or root) with type
operator fun IncompletePathBasedArtifact<FolderArtifact>.div(pt: PathBasedArtifactTypedPart): FolderArtifact = FolderArtifact(pt.type, path.resolve(pt.pathPart))
operator fun PathBasedArtifactRoot<FolderArtifact>.mod(pt: PathBasedArtifactTypedPart): FolderArtifact = FolderArtifact(pt.type, ctx.resolvePath(pt.pathPart))

// complete files artifact building functions
// from type + path/string/other path based artifact, based on context
//public fun ScriptContext.files(type: ArtifactType, path: Path): FileGlobArtifact = FileGlobArtifact(type, resolvePath(path), listOf(), listOf())
//public fun ScriptContext.files(type: ArtifactType, path: String): FileGlobArtifact = FileGlobArtifact(type, resolvePath(path), listOf(), listOf())
//public fun ScriptContext.files(type: ArtifactType, base: PathBasedArtifact): FileGlobArtifact = FileGlobArtifact(type, resolvePath(base.path), listOf(), listOf())
// same with additional include parameter
fun ScriptContext.files(type: ArtifactType, base: Path, vararg include: String): FileGlobArtifact = FileGlobArtifact(type, resolvePath(base), include.asIterable(), listOf())
fun ScriptContext.files(type: ArtifactType, base: String, vararg include: String): FileGlobArtifact = FileGlobArtifact(type, resolvePath(base), include.asIterable(), listOf())
fun ScriptContext.files(type: ArtifactType, base: PathBasedArtifact, vararg include: String): FileGlobArtifact = FileGlobArtifact(type, resolvePath(base.path), include.asIterable(), listOf())
// context independent variant with path as a base
fun files(type: ArtifactType, base: Path, vararg include: String): FileGlobArtifact = FileGlobArtifact(type, base, include.asIterable(), listOf())
// from typed root with first path part
operator fun TypedPathBasedArtifactRoot<FileGlobArtifact>.mod(p: Path): FileGlobArtifact = FileGlobArtifact(type, ctx.resolvePath(p), listOf(), listOf())
operator fun TypedPathBasedArtifactRoot<FileGlobArtifact>.mod(p: String): FileGlobArtifact = FileGlobArtifact(type, ctx.resolvePath(p), listOf(), listOf())
operator fun TypedPathBasedArtifactRoot<FileGlobArtifact>.mod(p: PathBasedArtifact): FileGlobArtifact = FileGlobArtifact(type, ctx.resolvePath(p.path), listOf(), listOf())
// from other files artifact - adding path elements
operator fun FileGlobArtifact.div(p: Path): FileGlobArtifact = FileGlobArtifact(type, path.resolve(p), included, excluded)
operator fun FileGlobArtifact.div(p: String): FileGlobArtifact = FileGlobArtifact(type, path.resolve(p), included, excluded)
// from other files artifact - including/excluding files
operator fun FileGlobArtifact.plus(g: String): FileGlobArtifact = FileGlobArtifact(type, path, included.append(g), excluded)
fun FileGlobArtifact.include(vararg gs: String): FileGlobArtifact = FileGlobArtifact(type, path, included.append(*gs), excluded)
operator fun FileGlobArtifact.minus(g: String): FileGlobArtifact = FileGlobArtifact(type, path, included, excluded.append(g))
fun FileGlobArtifact.exclude(vararg gs: String): FileGlobArtifact = FileGlobArtifact(type, path, included, excluded.append(*gs))


// incomplete files artifact building functions
// from type - typed root
fun ScriptContext.files(type: ArtifactType): TypedPathBasedArtifactRoot<FileGlobArtifact> = TypedPathBasedArtifactRoot(this, type)
// file artifact root creation ( *file* % "path" / "abc".jar )
fun ScriptContext.files(): PathBasedArtifactRoot<FileGlobArtifact> = PathBasedArtifactRoot(this)
val ScriptContext.files: PathBasedArtifactRoot<FileGlobArtifact> get() = files()


// completing incomplete file artifact (or root) with type
operator fun IncompletePathBasedArtifact<FileGlobArtifact>.div(pt: PathBasedArtifactTypedPart): FileGlobArtifact = FileGlobArtifact(pt.type, path.resolve(pt.pathPart), listOf(), listOf())
operator fun PathBasedArtifactRoot<FileGlobArtifact>.mod(pt: PathBasedArtifactTypedPart): FileGlobArtifact = FileGlobArtifact(pt.type, ctx.resolvePath(pt.pathPart), listOf(), listOf())


// \todo too common concept and/or name, propose for inclusion into stdlib or find other solution
internal fun<T> Iterable<T>.append(vararg vs: T): Iterable<T> {
    val lst = this.toMutableList()
    lst.addAll(vs)
    return lst
}


// artifacts set
fun ScriptContext.artifactsSet(vararg artifacts: Artifact) : ArtifactsSet = ArtifactsSet(artifacts.toList())
fun ScriptContext.artifactsSet(vararg artifacts: Any): ArtifactsSet = artifactsSet(artifacts.asIterable())


// variable
fun <T: Any> variable(type: ArtifactType, ref: T) : VariableArtifact<T> = VariableArtifact(type, ref)


// generic typed path part generators
fun String.type(at: ArtifactType): PathBasedArtifactTypedPart = PathBasedArtifactTypedPart(Paths.get(this), at)
fun Path.type(at: ArtifactType): PathBasedArtifactTypedPart = PathBasedArtifactTypedPart(this, at)


// concrete typed path part generators
val String.src: PathBasedArtifactTypedPart get() = type(artifacts.source)
fun String.src(extension: String): PathBasedArtifactTypedPart = type(artifacts.source(extension))
val Path.src: PathBasedArtifactTypedPart get() = type(artifacts.source)
fun Path.src(extension: String): PathBasedArtifactTypedPart = type(artifacts.source(extension))

val String.res: PathBasedArtifactTypedPart get() = type(artifacts.resource)
fun String.res(extension: String): PathBasedArtifactTypedPart = type(artifacts.resource(extension))
val Path.res: PathBasedArtifactTypedPart get() = type(artifacts.resource)
fun Path.res(extension: String): PathBasedArtifactTypedPart = type(artifacts.resource(extension))

val String.bin: PathBasedArtifactTypedPart get() = type(artifacts.binary)
fun String.bin(extension: String): PathBasedArtifactTypedPart = type(artifacts.binary(extension))
val Path.bin: PathBasedArtifactTypedPart get() = type(artifacts.binary)
fun Path.bin(extension: String): PathBasedArtifactTypedPart = type(artifacts.binary(extension))

val String.jar: PathBasedArtifactTypedPart get() = type(artifacts.jar)
fun String.jar(extension: String): PathBasedArtifactTypedPart = type(artifacts.jar(extension))
val Path.jar: PathBasedArtifactTypedPart get() = type(artifacts.jar)
fun Path.jar(extension: String): PathBasedArtifactTypedPart = type(artifacts.jar(extension))

val String.cfg: PathBasedArtifactTypedPart get() = type(artifacts.config)
fun String.cfg(extension: String): PathBasedArtifactTypedPart = type(artifacts.config(extension))
val Path.cfg: PathBasedArtifactTypedPart get() = type(artifacts.config)
fun Path.cfg(extension: String): PathBasedArtifactTypedPart = type(artifacts.config(extension))


// properties generation TypedPathBasedArtifactRoot from untyped root
val<T: PathBasedArtifact> PathBasedArtifactRoot<T>.src: TypedPathBasedArtifactRoot<T> get() = TypedPathBasedArtifactRoot(ctx, artifacts.source)
fun<T: PathBasedArtifact> PathBasedArtifactRoot<T>.src(extension: String): TypedPathBasedArtifactRoot<T> = TypedPathBasedArtifactRoot(ctx, artifacts.source(extension))
val<T: PathBasedArtifact> PathBasedArtifactRoot<T>.res: TypedPathBasedArtifactRoot<T> get() = TypedPathBasedArtifactRoot(ctx, artifacts.resource)
fun<T: PathBasedArtifact> PathBasedArtifactRoot<T>.res(extension: String): TypedPathBasedArtifactRoot<T> = TypedPathBasedArtifactRoot(ctx, artifacts.resource(extension))
val<T: PathBasedArtifact> PathBasedArtifactRoot<T>.bin: TypedPathBasedArtifactRoot<T> get() = TypedPathBasedArtifactRoot(ctx, artifacts.binary)
fun<T: PathBasedArtifact> PathBasedArtifactRoot<T>.bin(extension: String): TypedPathBasedArtifactRoot<T> = TypedPathBasedArtifactRoot(ctx, artifacts.binary(extension))
val<T: PathBasedArtifact> PathBasedArtifactRoot<T>.jar: TypedPathBasedArtifactRoot<T> get() = TypedPathBasedArtifactRoot(ctx, artifacts.jar)
fun<T: PathBasedArtifact> PathBasedArtifactRoot<T>.jar(extension: String): TypedPathBasedArtifactRoot<T> = TypedPathBasedArtifactRoot(ctx, artifacts.jar(extension))
val<T: PathBasedArtifact> PathBasedArtifactRoot<T>.cfg: TypedPathBasedArtifactRoot<T> get() = TypedPathBasedArtifactRoot(ctx, artifacts.config)
fun<T: PathBasedArtifact> PathBasedArtifactRoot<T>.cfg(extension: String): TypedPathBasedArtifactRoot<T> = TypedPathBasedArtifactRoot(ctx, artifacts.config(extension))
