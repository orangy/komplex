package komplex.dsl

import komplex.utils.resolvePath
import java.nio.file.Path
import java.nio.file.Paths

// generic path manipulation
public fun Path.div(p: Path): Path = resolve(p)
public fun Path.div(p: String): Path = resolve(p)

// set of functions and operators to simplify artifacts description
// examples
// file % "path" / "abc".jar
// folder.bin % "path"
// files.jar % "path" / "*.jar"


// represent a particular Artifact kind being built (captured as T) when type is not specified yet
// separate type for beginning of the artifact expression, allowing only % operation on it
// context is needed for subsequent path resolving from a root folder
public class PathBasedArtifactRoot<T: PathBasedArtifact>(public val ctx: ScriptContext) {}


// represent a particular Artifact kind being built (captured as T) when type is specified right after the PathBasedArtifactRoot
// separate type created from PathBasedArtifactRoot, allowing only % operation on it
// context is needed for subsequent path resolving from a root folder
public class TypedPathBasedArtifactRoot<T: PathBasedArtifact>(public val ctx: ScriptContext, public val type: ArtifactType) {}


// represent a particular Artifact kind being built (captured as T) when type is not specified yet
// separate type for middle of the artifact expression, allowing only / operation
public class IncompletePathBasedArtifact<T: PathBasedArtifact>(public val path: Path) {}


// represent typed ending of the artifact expression, passing it to the right argument of / operator yelds a ready artifact
public class PathBasedArtifactTypedPart(public val pathPart: Path, public val type: ArtifactType) {}


// generic path-based artifact functions
// from root - create incomplete artifact by adding path/string/existing artifact's path to root
public fun<T: PathBasedArtifact> PathBasedArtifactRoot<T>.mod(p: Path): IncompletePathBasedArtifact<T> = IncompletePathBasedArtifact(ctx.resolvePath(p))
public fun<T: PathBasedArtifact> PathBasedArtifactRoot<T>.mod(p: String): IncompletePathBasedArtifact<T> = IncompletePathBasedArtifact(ctx.resolvePath(p))
public fun<T: PathBasedArtifact> PathBasedArtifactRoot<T>.mod(p: PathBasedArtifact): IncompletePathBasedArtifact<T> = IncompletePathBasedArtifact(ctx.resolvePath(p.path))

// from typed root - not generic, see definitions for every artifact type
// \todo seek for generic solution

// from incomplete artifact - create incomplete artifact by adding path/string/existing artifact's path to base incomplete artifact
public fun<T: PathBasedArtifact> IncompletePathBasedArtifact<T>.div(p: Path): IncompletePathBasedArtifact<T> = IncompletePathBasedArtifact(path.resolve(p))
public fun<T: PathBasedArtifact> IncompletePathBasedArtifact<T>.div(p: String): IncompletePathBasedArtifact<T> = IncompletePathBasedArtifact(path.resolve(p))
public fun<T: PathBasedArtifact> IncompletePathBasedArtifact<T>.div(p: PathBasedArtifact): IncompletePathBasedArtifact<T> = IncompletePathBasedArtifact(path.resolve(p.path))


// complete file artifact building functions
// from type + path/string, based on context
public fun ScriptContext.file(type: ArtifactType, path: Path): FileArtifact = SimpleFileArtifact(type, resolvePath(path))
public fun ScriptContext.file(type: ArtifactType, path: String): FileArtifact = SimpleFileArtifact(type, resolvePath(path))
// from typed root
public fun TypedPathBasedArtifactRoot<FileArtifact>.mod(p: Path): FileArtifact = SimpleFileArtifact(type, ctx.resolvePath(p))
public fun TypedPathBasedArtifactRoot<FileArtifact>.mod(p: String): FileArtifact = SimpleFileArtifact(type, ctx.resolvePath(p))
public fun TypedPathBasedArtifactRoot<FileArtifact>.mod(p: PathBasedArtifact): FileArtifact = SimpleFileArtifact(type, ctx.resolvePath(p.path))
// from other file artifact
public fun FileArtifact.div(p: Path): FileArtifact = SimpleFileArtifact(type, path.resolve(p))
public fun FileArtifact.div(p: String): FileArtifact = SimpleFileArtifact(type, path.resolve(p))


// incomplete file artifact building functions
// from type - typed root
public fun ScriptContext.file(type: ArtifactType): TypedPathBasedArtifactRoot<FileArtifact> = TypedPathBasedArtifactRoot(this, type)
// 0-ary - root
public fun ScriptContext.file(): PathBasedArtifactRoot<FileArtifact> = PathBasedArtifactRoot(this)
public val ScriptContext.file: PathBasedArtifactRoot<FileArtifact> get() = file()

// completing incomplete file artifact (or root) with type
public fun IncompletePathBasedArtifact<FileArtifact>.div(pt: PathBasedArtifactTypedPart): FileArtifact = SimpleFileArtifact(pt.type, path.resolve(pt.pathPart))
public fun PathBasedArtifactRoot<FileArtifact>.mod(pt: PathBasedArtifactTypedPart): FileArtifact = SimpleFileArtifact(pt.type, ctx.resolvePath(pt.pathPart))


// \todo add extrensions handling for folder and files artifacts

// complete folder artifact building functions
// from type + path/string, based on context
public fun ScriptContext.folder(type: ArtifactType, path: Path): FolderArtifact =  FolderArtifact(type, resolvePath(path))
public fun ScriptContext.folder(type: ArtifactType, path: String): FolderArtifact = FolderArtifact(type, resolvePath(path))
// from typed root
public fun TypedPathBasedArtifactRoot<FolderArtifact>.mod(p: Path): FolderArtifact = FolderArtifact(type, ctx.resolvePath(p))
public fun TypedPathBasedArtifactRoot<FolderArtifact>.mod(p: String): FolderArtifact = FolderArtifact(type, ctx.resolvePath(p))
public fun TypedPathBasedArtifactRoot<FolderArtifact>.mod(p: PathBasedArtifact): FolderArtifact = FolderArtifact(type, ctx.resolvePath(p.path))
// from other folder artifact
public fun FolderArtifact.div(p: Path): FolderArtifact = FolderArtifact(type, path.resolve(p))
public fun FolderArtifact.div(p: String): FolderArtifact = FolderArtifact(type, path.resolve(p))


// incomplete folder artifact building functions
// from type - typed root
public fun ScriptContext.folder(type: ArtifactType): TypedPathBasedArtifactRoot<FolderArtifact> = TypedPathBasedArtifactRoot(this, type)
// file artifact root creation ( *folder % "path" / "abc".jar )
public fun ScriptContext.folder(): PathBasedArtifactRoot<FolderArtifact> = PathBasedArtifactRoot(this)
public val ScriptContext.folder: PathBasedArtifactRoot<FolderArtifact> get() = folder()

// completing incomplete file artifact (or root) with type
public fun IncompletePathBasedArtifact<FolderArtifact>.div(pt: PathBasedArtifactTypedPart): FolderArtifact = FolderArtifact(pt.type, path.resolve(pt.pathPart))
public fun PathBasedArtifactRoot<FolderArtifact>.mod(pt: PathBasedArtifactTypedPart): FolderArtifact = FolderArtifact(pt.type, ctx.resolvePath(pt.pathPart))

// complete files artifact building functions
// from type + path/string/other path based artifact, based on context
//public fun ScriptContext.files(type: ArtifactType, path: Path): FileGlobArtifact = FileGlobArtifact(type, resolvePath(path), listOf(), listOf())
//public fun ScriptContext.files(type: ArtifactType, path: String): FileGlobArtifact = FileGlobArtifact(type, resolvePath(path), listOf(), listOf())
//public fun ScriptContext.files(type: ArtifactType, base: PathBasedArtifact): FileGlobArtifact = FileGlobArtifact(type, resolvePath(base.path), listOf(), listOf())
// same with additional include parameter
public fun ScriptContext.files(type: ArtifactType, base: Path, vararg include: String): FileGlobArtifact = FileGlobArtifact(type, resolvePath(base), include.asIterable(), listOf())
public fun ScriptContext.files(type: ArtifactType, base: String, vararg include: String): FileGlobArtifact = FileGlobArtifact(type, resolvePath(base), include.asIterable(), listOf())
public fun ScriptContext.files(type: ArtifactType, base: PathBasedArtifact, vararg include: String): FileGlobArtifact = FileGlobArtifact(type, resolvePath(base.path), include.asIterable(), listOf())
// context independent variant with path as a base
public fun files(type: ArtifactType, base: Path, vararg include: String): FileGlobArtifact = FileGlobArtifact(type, base, include.asIterable(), listOf())
// from typed root with first path part
public fun TypedPathBasedArtifactRoot<FileGlobArtifact>.mod(p: Path): FileGlobArtifact = FileGlobArtifact(type, ctx.resolvePath(p), listOf(), listOf())
public fun TypedPathBasedArtifactRoot<FileGlobArtifact>.mod(p: String): FileGlobArtifact = FileGlobArtifact(type, ctx.resolvePath(p), listOf(), listOf())
public fun TypedPathBasedArtifactRoot<FileGlobArtifact>.mod(p: PathBasedArtifact): FileGlobArtifact = FileGlobArtifact(type, ctx.resolvePath(p.path), listOf(), listOf())
// from other files artifact - adding path elements
public fun FileGlobArtifact.div(p: Path): FileGlobArtifact = FileGlobArtifact(type, path.resolve(p), included, excluded)
public fun FileGlobArtifact.div(p: String): FileGlobArtifact = FileGlobArtifact(type, path.resolve(p), included, excluded)
// from other files artifact - including/excluding files
public fun FileGlobArtifact.plus(g: String): FileGlobArtifact = FileGlobArtifact(type, path, included.append(g), excluded)
public fun FileGlobArtifact.include(vararg gs: String): FileGlobArtifact = FileGlobArtifact(type, path, included.append(*gs), excluded)
public fun FileGlobArtifact.minus(g: String): FileGlobArtifact = FileGlobArtifact(type, path, included, excluded.append(g))
public fun FileGlobArtifact.exclude(vararg gs: String): FileGlobArtifact = FileGlobArtifact(type, path, included, excluded.append(*gs))


// incomplete files artifact building functions
// from type - typed root
public fun ScriptContext.files(type: ArtifactType): TypedPathBasedArtifactRoot<FileGlobArtifact> = TypedPathBasedArtifactRoot(this, type)
// file artifact root creation ( *file* % "path" / "abc".jar )
public fun ScriptContext.files(): PathBasedArtifactRoot<FileGlobArtifact> = PathBasedArtifactRoot(this)
public val ScriptContext.files: PathBasedArtifactRoot<FileGlobArtifact> get() = files()


// completing incomplete file artifact (or root) with type
public fun IncompletePathBasedArtifact<FileGlobArtifact>.div(pt: PathBasedArtifactTypedPart): FileGlobArtifact = FileGlobArtifact(pt.type, path.resolve(pt.pathPart), listOf(), listOf())
public fun PathBasedArtifactRoot<FileGlobArtifact>.mod(pt: PathBasedArtifactTypedPart): FileGlobArtifact = FileGlobArtifact(pt.type, ctx.resolvePath(pt.pathPart), listOf(), listOf())


// \todo too common concept and/or name, propose for inclusion into stdlib or find other solution
internal fun<T> Iterable<T>.append(vararg vs: T): Iterable<T> {
    val lst = toArrayList()
    lst.addAll(vs)
    return lst
}


// artifacts set
public fun ScriptContext.artifactsSet(vararg artifacts: Artifact) : ArtifactsSet = ArtifactsSet(artifacts.toArrayList())
public fun ScriptContext.artifactsSet(vararg artifacts: Any): ArtifactsSet = artifactsSet(artifacts.asIterable())


// variable
public fun variable<T: Any>(type: ArtifactType, ref: T) : VariableArtifact<T> = VariableArtifact(type, ref)


// generic typed path part generators
public fun String.type(at: ArtifactType): PathBasedArtifactTypedPart = PathBasedArtifactTypedPart(Paths.get(this), at)
public fun Path.type(at: ArtifactType): PathBasedArtifactTypedPart = PathBasedArtifactTypedPart(this, at)


// concrete typed path part generators
public val String.src: PathBasedArtifactTypedPart get() = type(artifacts.source)
public fun String.src(extension: String): PathBasedArtifactTypedPart = type(artifacts.source(extension))
public val Path.src: PathBasedArtifactTypedPart get() = type(artifacts.source)
public fun Path.src(extension: String): PathBasedArtifactTypedPart = type(artifacts.source(extension))

public val String.res: PathBasedArtifactTypedPart get() = type(artifacts.resource)
public fun String.res(extension: String): PathBasedArtifactTypedPart = type(artifacts.resource(extension))
public val Path.res: PathBasedArtifactTypedPart get() = type(artifacts.resource)
public fun Path.res(extension: String): PathBasedArtifactTypedPart = type(artifacts.resource(extension))

public val String.bin: PathBasedArtifactTypedPart get() = type(artifacts.binary)
public fun String.bin(extension: String): PathBasedArtifactTypedPart = type(artifacts.binary(extension))
public val Path.bin: PathBasedArtifactTypedPart get() = type(artifacts.binary)
public fun Path.bin(extension: String): PathBasedArtifactTypedPart = type(artifacts.binary(extension))

public val String.jar: PathBasedArtifactTypedPart get() = type(artifacts.jar)
public fun String.jar(extension: String): PathBasedArtifactTypedPart = type(artifacts.jar(extension))
public val Path.jar: PathBasedArtifactTypedPart get() = type(artifacts.jar)
public fun Path.jar(extension: String): PathBasedArtifactTypedPart = type(artifacts.jar(extension))

public val String.cfg: PathBasedArtifactTypedPart get() = type(artifacts.config)
public fun String.cfg(extension: String): PathBasedArtifactTypedPart = type(artifacts.config(extension))
public val Path.cfg: PathBasedArtifactTypedPart get() = type(artifacts.config)
public fun Path.cfg(extension: String): PathBasedArtifactTypedPart = type(artifacts.config(extension))


// properties generation TypedPathBasedArtifactRoot from untyped root
public val<T: PathBasedArtifact> PathBasedArtifactRoot<T>.src: TypedPathBasedArtifactRoot<T> get() = TypedPathBasedArtifactRoot(ctx, artifacts.source)
public fun<T: PathBasedArtifact> PathBasedArtifactRoot<T>.src(extension: String): TypedPathBasedArtifactRoot<T> = TypedPathBasedArtifactRoot(ctx, artifacts.source(extension))
public val<T: PathBasedArtifact> PathBasedArtifactRoot<T>.res: TypedPathBasedArtifactRoot<T> get() = TypedPathBasedArtifactRoot(ctx, artifacts.resource)
public fun<T: PathBasedArtifact> PathBasedArtifactRoot<T>.res(extension: String): TypedPathBasedArtifactRoot<T> = TypedPathBasedArtifactRoot(ctx, artifacts.resource(extension))
public val<T: PathBasedArtifact> PathBasedArtifactRoot<T>.bin: TypedPathBasedArtifactRoot<T> get() = TypedPathBasedArtifactRoot(ctx, artifacts.binary)
public fun<T: PathBasedArtifact> PathBasedArtifactRoot<T>.bin(extension: String): TypedPathBasedArtifactRoot<T> = TypedPathBasedArtifactRoot(ctx, artifacts.binary(extension))
public val<T: PathBasedArtifact> PathBasedArtifactRoot<T>.jar: TypedPathBasedArtifactRoot<T> get() = TypedPathBasedArtifactRoot(ctx, artifacts.jar)
public fun<T: PathBasedArtifact> PathBasedArtifactRoot<T>.jar(extension: String): TypedPathBasedArtifactRoot<T> = TypedPathBasedArtifactRoot(ctx, artifacts.jar(extension))
public val<T: PathBasedArtifact> PathBasedArtifactRoot<T>.cfg: TypedPathBasedArtifactRoot<T> get() = TypedPathBasedArtifactRoot(ctx, artifacts.config)
public fun<T: PathBasedArtifact> PathBasedArtifactRoot<T>.cfg(extension: String): TypedPathBasedArtifactRoot<T> = TypedPathBasedArtifactRoot(ctx, artifacts.config(extension))
