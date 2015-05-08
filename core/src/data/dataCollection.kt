package komplex.data

import komplex.dsl.ArtifactType
import komplex.dsl.FolderArtifact
import komplex.model.ArtifactData
import komplex.model.nicePrint
import java.util.HashSet
import komplex.utils.findFilesInPath
import java.nio.file.Path
import komplex.utils.findGlobFiles
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.Arrays
import java.util.Comparator
import kotlin.properties.Delegates

public fun ByteArray.hashEquals(other: ByteArray): Boolean = Arrays.equals(this, other)

public fun mergeHashes(artifacts: Iterable<ArtifactData?>): ByteArray {
    val digest = MessageDigest.getInstance("SHA-1")
    val comp = object : Comparator<ArtifactData?> {
        override fun compare(o1: ArtifactData?, o2: ArtifactData?): Int = when {
            o1 == o2 -> 0 // same object or nulls
            o1 == null -> -1
            o2 == null -> 1
            o1.hash.size() < o2.hash.size() -> -1
            o1.hash.size() > o2.hash.size() -> 1
            else -> {
                val mismatch = o1.hash.asIterable().zip(o2.hash.asIterable()).firstOrNull { it.first != it.second }
                if (mismatch == null) 0
                else mismatch.first.compareTo(mismatch.second)
            }
        }
    }
    artifacts.sortBy(comp).forEach { if (it != null) digest.update(it.hash) }
    return digest.digest()
}


public trait DataCollection<Data: ArtifactData> : ArtifactData {
    public val coll: Iterable<Data>
}

public open class DataSet<Data: ArtifactData>(override val coll: HashSet<Data> = hashSetOf()) : DataCollection<Data> {
    override val hash: ByteArray by Delegates.lazy { mergeHashes(coll)}
    override val sourcesHash: ByteArray? = null
}

// second constructor
public fun DataSet<Data: ArtifactData>(coll: Iterable<Data>): DataSet<Data> = DataSet(coll.toHashSet())


public open class DataToDataCollectionAdaptor<Data: ArtifactData>(data: Data) : DataCollection<Data> {
    override val hash: ByteArray by Delegates.lazy { mergeHashes(coll)}
    override val sourcesHash: ByteArray? = null
    override val coll: Iterable<Data> = listOf(data)
}

public fun adaptToDataCollection<D: ArtifactData>(coll: DataCollection<D>) : DataCollection<D> = coll
public fun adaptToDataCollection<D: ArtifactData>(data: D) : DataCollection<D> = DataToDataCollectionAdaptor(data)


private fun collectArtifactFiles(artifacts: Iterable<komplex.dsl.FileArtifact>): Iterable<FileData> =
        artifacts.map { SimpleFileData(it.path) }

private fun collectFolderFiles(folder: Path): Iterable<SimpleFileData> =
        findFilesInPath(folder).map { SimpleFileData(it) }

private fun collectGlobFiles(included: Iterable<String>, excluded: Iterable<String>, baseDir: Path?): Iterable<FileData> =
        findGlobFiles(included, excluded, baseDir).map { SimpleFileData(it) }

private fun collectFiles(paths: Iterable<Path>): Iterable<FileData> =
        paths.map { SimpleFileData(it) }


public enum class OpenFileSet {
    Nothing
    FoldersAsLibraries
}

public fun openFileSet(pair: Pair<komplex.model.ArtifactDesc, ArtifactData?>,
                       baseDir: Path? = null,
                       options: OpenFileSet = OpenFileSet.Nothing
): DataCollection<FileData> {
    val fst = pair.first
    val snd = pair.second
    return when (snd) {
        is DataSet<*> -> if (snd.coll.isEmpty() || snd.coll.first() !is FileData ||
                                // special treatment of folders as libraries
                                // \todo find better solution, e.g. dispatching by artifact type and separate functions like openLibrariesSet
                                (options == OpenFileSet.FoldersAsLibraries && fst is FolderArtifact && fst.type == komplex.dsl.artifacts.binaries))
                            openFileSet(fst, baseDir = baseDir, options = options)
                         else
                            adaptToDataCollection(snd as DataCollection<FileData>)
        is FileData -> adaptToDataCollection(snd)
        else -> openFileSet(fst, baseDir = baseDir, options = options)
    }
}

public fun openFileSet(vararg artifacts: komplex.model.ArtifactDesc,
                       baseDir: Path? = null,
                       options: OpenFileSet = OpenFileSet.Nothing
): DataSet<FileData> =
    // emulating dynamic dispatching with extension methods
    // \todo find more elegant solution
    DataSet(artifacts.flatMap {
        when (it) {
            // note: sequence matters!
            is komplex.dsl.FileGlobArtifact -> collectGlobFiles(it.included, it.excluded, baseDir = (baseDir ?: Paths.get(".")).resolve(it.path))
            is komplex.dsl.FolderArtifact ->
                // special treatment of folders as libraries
                // \todo find better solution, see openFileSet(pair...)
                if (options == OpenFileSet.FoldersAsLibraries && it.type == komplex.dsl.artifacts.binaries ) listOf(FolderData(it.path))
                else collectFolderFiles((baseDir ?: Paths.get(".")).resolve(it.path))
            is komplex.dsl.FileArtifact -> listOf(SimpleFileData(it.path))
            is Path -> listOf(SimpleFileData(it))
            is DataSet<*> -> it.coll as Iterable<FileData>
            is FileData -> listOf(it)
            else -> throw UnsupportedOperationException("cannot open ${artifacts.firstOrNull()?.name ?: "[]"}... as FileSet")
        }})


public fun openFileSetI(paths: Iterable<Path>): DataSet<FileData> = DataSet( paths.map { SimpleFileData(it) })


// \todo consider implementing direct open functions for stream sets as for file sets

public fun openInputStreamSet(fileColl: DataCollection<FileData>): DataSet<InputStreamData> =
        DataSet<InputStreamData>( fileColl.coll.map { openInputStream(it) }.toHashSet())

public fun openOutputStreamSet(fileColl: DataCollection<FileData>): DataSet<OutputStreamData> =
        DataSet<OutputStreamData>( fileColl.coll.map { openOutputStream(it) }.toHashSet())

