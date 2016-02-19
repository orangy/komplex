package komplex.data

import komplex.dsl.FileArtifact
import komplex.dsl.FileGlobArtifact
import komplex.dsl.FolderArtifact
import komplex.model.ArtifactData
import komplex.utils.findFilesInPath
import komplex.utils.findGlobFiles
import komplex.utils.resolve
import java.nio.file.Path
import java.security.MessageDigest

fun fileHash(path: Path): ByteArray {
    val digest = MessageDigest.getInstance("SHA-1")
    path.toFile().forEachBlock { bytes, i -> digest.update(bytes) }
    return digest.digest()
}

interface FileData : komplex.model.ArtifactData {
    val path: Path
}

open class SimpleFileData(ipath: Path) : FileData {
    override val path: Path = ipath.normalize().toAbsolutePath()
    override val hash: ByteArray by lazy { fileHash(path) }
}

fun getFile(artifact: komplex.dsl.FileArtifact): FileData = SimpleFileData(artifact.path)

class FolderData(ipath: Path) : FileData {
    override val path: Path = ipath.normalize().toAbsolutePath()
    override val hash: ByteArray by lazy { mergeHashes(collectFolderFiles(path)) }
}


private fun collectArtifactFiles(artifacts: Iterable<komplex.dsl.FileArtifact>): Iterable<FileData> =
        artifacts.map { SimpleFileData(it.path) }

private fun collectFolderFiles(folder: Path): Iterable<SimpleFileData> =
        findFilesInPath(folder).map { SimpleFileData(it) }

private fun collectGlobFiles(included: Iterable<String>, excluded: Iterable<String>, baseDir: Path?): Iterable<FileData> =
        findGlobFiles(included, excluded, baseDir).map { SimpleFileData(it) }

private fun collectFiles(paths: Iterable<Path>): Iterable<FileData> =
        paths.map { SimpleFileData(it) }


enum class OpenFileSet {
    Nothing,
    FoldersAsLibraries
}

fun openFileSet(pair: Pair<komplex.model.ArtifactDesc, ArtifactData?>,
                       baseDir: Path? = null,
                       options: OpenFileSet = OpenFileSet.Nothing
): DataCollection<FileData> {
    val fst = pair.first
    val snd = pair.second
    return when (snd) {
        is DataSet<*> ->
            if (snd.coll.isEmpty() || snd.coll.first() !is FileData ||
                // special treatment of folders as libraries
                // \todo find better solution, e.g. dispatching by artifact type and separate functions like openLibrariesSet
                (options == OpenFileSet.FoldersAsLibraries && fst is FolderArtifact && fst.type == komplex.dsl.artifacts.binary))
                    openFileSet(fst, baseDir = baseDir, options = options)
            else
                adaptToDataCollection(snd as DataCollection<FileData>)
        is FileData -> adaptToDataCollection(snd)
        else -> openFileSet(fst, baseDir = baseDir, options = options)
    }
}

internal fun tryOpenFileSet(it: Any, baseDir: Path? = null, options: OpenFileSet = OpenFileSet.Nothing): Iterable<FileData>? {
    return when (it) {
    // note: sequence matters!
        is FileGlobArtifact -> collectGlobFiles(it.included, it.excluded, baseDir = baseDir.resolve(it.path))
        is FolderArtifact ->
            // special treatment of folders as libraries
            // \todo find better solution, see openFileSet(pair...)
            if (options == OpenFileSet.FoldersAsLibraries && it.type == komplex.dsl.artifacts.binary ) listOf(FolderData(it.path))
            else collectFolderFiles(baseDir.resolve(it.path))
        is FileArtifact -> listOf(SimpleFileData(it.path))
        is Path -> listOf(SimpleFileData(it))
        is DataSet<*> -> it.coll as? Iterable<FileData>
        is FileData -> listOf(it)
        else -> null
    }
}

fun openFileSet(artifacts: Iterable<komplex.model.ArtifactDesc>,
                       baseDir: Path? = null,
                       options: OpenFileSet = OpenFileSet.Nothing
): DataSet<FileData> =
        // emulating dynamic dispatching with extension methods
        // \todo find more elegant solution
        DataSet(artifacts.flatMap {
            tryOpenFileSet(it, baseDir, options)
                ?: throw UnsupportedOperationException("cannot open ${artifacts.firstOrNull()?.name ?: "[]"}... as FileSet")
        })

fun openFileSet(vararg artifacts: komplex.model.ArtifactDesc,
                       baseDir: Path? = null,
                       options: OpenFileSet = OpenFileSet.Nothing
): DataSet<FileData> = openFileSet(artifacts.asIterable(), baseDir, options)

fun openFileSetI(paths: Iterable<Path>): DataSet<FileData> = DataSet( paths.map { SimpleFileData(it) })


// \todo consider implementing direct open functions for stream sets as for file sets

fun openInputStreamSet(fileColl: DataCollection<FileData>): DataSet<InputStreamData> =
        DataSet<InputStreamData>( fileColl.coll.map { openInputStream(it) }.toHashSet())

fun openOutputStreamSet(fileColl: DataCollection<FileData>): DataSet<OutputStreamData> =
        DataSet<OutputStreamData>( fileColl.coll.map { openOutputStream(it) }.toHashSet())

