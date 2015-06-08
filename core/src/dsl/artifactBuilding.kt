package komplex.dsl

import komplex.utils.resolvePath
import java.nio.file.Path
import java.nio.file.Paths

// generic path manipulation
public fun Path.div(p: Path): Path = this.resolve(p)
public fun Path.div(p: String): Path = this.resolve(p)


public class PathBasedArtifactRoot<T: PathBasedArtifact>(public val path: Path) {}

public class IncompletePathBasedArtifact<T: PathBasedArtifact>(public val path: Path) {}

public class PathBasedArtifactTypedPart(public val pathPart: Path, public val type: ArtifactType) {}

// set of functions and operators to simplify artifacts description
// e.g. use file % "path" / "abc".jar

// generic path-based artifact functions
// from root - create incomplete artifact by adding path/string/existing artifact's path to root
public fun<T: PathBasedArtifact> PathBasedArtifactRoot<T>.mod(p: Path): IncompletePathBasedArtifact<T> = IncompletePathBasedArtifact(this.path.resolve(p))
public fun<T: PathBasedArtifact> PathBasedArtifactRoot<T>.mod(p: String): IncompletePathBasedArtifact<T> = IncompletePathBasedArtifact(this.path.resolve(p))
public fun<T: PathBasedArtifact> PathBasedArtifactRoot<T>.mod(p: PathBasedArtifact): IncompletePathBasedArtifact<T> = IncompletePathBasedArtifact(this.path.resolve(p.path))

// from incomplete artifact - create incomplete artifact by adding path/string/existing artifact's path to base incomplete artifact
public fun<T: PathBasedArtifact> IncompletePathBasedArtifact<T>.div(p: Path): IncompletePathBasedArtifact<T> = IncompletePathBasedArtifact(this.path.resolve(p))
public fun<T: PathBasedArtifact> IncompletePathBasedArtifact<T>.div(p: String): IncompletePathBasedArtifact<T> = IncompletePathBasedArtifact(this.path.resolve(p))
public fun<T: PathBasedArtifact> IncompletePathBasedArtifact<T>.div(p: PathBasedArtifact): IncompletePathBasedArtifact<T> = IncompletePathBasedArtifact(this.path.resolve(p.path))


// file artifact functions
// complete file artifact creation
// from type + path/string, based on context
public fun ScriptContext.file(type: ArtifactType, path: Path): FileArtifact = SimpleFileArtifact(type, this.resolvePath(path))
public fun ScriptContext.file(type: ArtifactType, path: String): FileArtifact = SimpleFileArtifact(type, this.resolvePath(path))
// from type and basepath from context
public fun ScriptContext.file(type: ArtifactType): FileArtifact = SimpleFileArtifact(type, this.resolvePath("."))
// from other file artifact
public fun FileArtifact.div(p: Path): FileArtifact = SimpleFileArtifact(this.type, this.path.resolve(p))

// file artifact root creation ( *file* % "path" / "abc".jar )
public fun ScriptContext.file(): PathBasedArtifactRoot<FileArtifact> = PathBasedArtifactRoot(this.resolvePath("."))
public val ScriptContext.file: PathBasedArtifactRoot<FileArtifact> get() = this.file()

// completing incomplete file artifact (or root) with type
public fun IncompletePathBasedArtifact<FileArtifact>.div(pt: PathBasedArtifactTypedPart): FileArtifact = SimpleFileArtifact(pt.type, this.path.resolve(pt.pathPart))
public fun PathBasedArtifactRoot<FileArtifact>.mod(pt: PathBasedArtifactTypedPart): FileArtifact = SimpleFileArtifact(pt.type, this.path.resolve(pt.pathPart))


// folder artifact functions
// complete folder artifact creation
// from type + path/string, based on context
public fun ScriptContext.folder(type: ArtifactType, path: Path): FolderArtifact =  FolderArtifact(type, this.resolvePath(path))
public fun ScriptContext.folder(type: ArtifactType, path: String): FolderArtifact = FolderArtifact(type, this.resolvePath(path))
// from type and basepath from context
public fun ScriptContext.folder(type: ArtifactType): FolderArtifact = FolderArtifact(type, this.resolvePath("."))
// from other folder artifact
public fun FolderArtifact.div(p: Path): FolderArtifact = FolderArtifact(this.type, this.path.resolve(p))

// file artifact root creation ( *folder % "path" / "abc".jar )
public fun ScriptContext.folder(): PathBasedArtifactRoot<FolderArtifact> = PathBasedArtifactRoot(this.resolvePath("."))
public val ScriptContext.folder: PathBasedArtifactRoot<FolderArtifact> get() = this.folder()

// completing incomplete file artifact (or root) with type
public fun IncompletePathBasedArtifact<FolderArtifact>.div(pt: PathBasedArtifactTypedPart): FolderArtifact = FolderArtifact(pt.type, this.path.resolve(pt.pathPart))
public fun PathBasedArtifactRoot<FolderArtifact>.mod(pt: PathBasedArtifactTypedPart): FolderArtifact = FolderArtifact(pt.type, this.path.resolve(pt.pathPart))


// files group artifact functions
// complete files artifact creation
// from type + path/string/other path based artifact, based on context
public fun ScriptContext.files(type: ArtifactType, path: Path): FileGlobArtifact = FileGlobArtifact(type, this.resolvePath(path))
public fun ScriptContext.files(type: ArtifactType, path: String): FileGlobArtifact = FileGlobArtifact(type, this.resolvePath(path))
public fun ScriptContext.files(type: ArtifactType, base: PathBasedArtifact): FileGlobArtifact = FileGlobArtifact(`type`, this.resolvePath(base.path))
// same with additional include parameter
public fun ScriptContext.files(type: ArtifactType, base: Path, include: String): FileGlobArtifact = files(type, base).include(include)
public fun ScriptContext.files(type: ArtifactType, base: String, include: String): FileGlobArtifact = files(type, base).include(include)
public fun ScriptContext.files(type: ArtifactType, base: PathBasedArtifact, include: String): FileGlobArtifact = files(type,base).include(include)
// context independent variant with path as a base
public fun files(type: ArtifactType, base: Path, include: String): FileGlobArtifact = FileGlobArtifact(`type`, base).include(include)

// from type and basepath from context
public fun ScriptContext.files(type: ArtifactType): FileGlobArtifact = FileGlobArtifact(type, this.resolvePath("."))
// from other files artifact
public fun FileGlobArtifact.div(p: Path): FileGlobArtifact = FileGlobArtifact(this.type, this.path.resolve(p))

// file artifact root creation ( *file* % "path" / "abc".jar )
public fun ScriptContext.files(): PathBasedArtifactRoot<FileGlobArtifact> = PathBasedArtifactRoot(this.resolvePath("."))
public val ScriptContext.files: PathBasedArtifactRoot<FileGlobArtifact> get() = this.files()

// completing incomplete file artifact (or root) with type
public fun IncompletePathBasedArtifact<FileGlobArtifact>.div(pt: PathBasedArtifactTypedPart): FileGlobArtifact = FileGlobArtifact(pt.type, this.path.resolve(pt.pathPart))
public fun PathBasedArtifactRoot<FileGlobArtifact>.mod(pt: PathBasedArtifactTypedPart): FileGlobArtifact = FileGlobArtifact(pt.type, this.path.resolve(pt.pathPart))

// additional operators for including/excluding files
public fun FileGlobArtifact.plus(g: String): FileGlobArtifact = this.include(g)
public fun FileGlobArtifact.minus(g: String): FileGlobArtifact = this.exclude(g)


// artifacts set
public fun ScriptContext.artifactsSet(vararg artifacts: Artifact) : ArtifactsSet = ArtifactsSet(artifacts.toArrayList())
public fun ScriptContext.artifactsSet(vararg artifacts: Any): ArtifactsSet = artifactsSet(artifacts.asIterable())


// variable
public fun variable<T: Any>(type: ArtifactType, ref: T) : VariableArtifact<T> = VariableArtifact(type, ref)


// generic typed path part generators
public fun String.type(at: ArtifactType): PathBasedArtifactTypedPart = PathBasedArtifactTypedPart(Paths.get(this), at)
public fun Path.type(at: ArtifactType): PathBasedArtifactTypedPart = PathBasedArtifactTypedPart(this, at)

public val String.source: PathBasedArtifactTypedPart get() = this.type(artifacts.sources)
public val Path.source: PathBasedArtifactTypedPart get() = this.type(artifacts.sources)
public val String.resource: PathBasedArtifactTypedPart get() = this.type(artifacts.resources)
public val Path.resource: PathBasedArtifactTypedPart get() = this.type(artifacts.resources)
public val String.bin: PathBasedArtifactTypedPart get() = this.type(artifacts.binaries)
public val Path.bin: PathBasedArtifactTypedPart get() = this.type(artifacts.binaries)
public val String.jar: PathBasedArtifactTypedPart get() = this.type(artifacts.jar)
public val Path.jar: PathBasedArtifactTypedPart get() = this.type(artifacts.jar)
public val String.config: PathBasedArtifactTypedPart get() = this.type(artifacts.configs)
public val Path.config: PathBasedArtifactTypedPart get() = this.type(artifacts.configs)

