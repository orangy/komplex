package komplex.data

import komplex.model.ArtifactData
import java.util.HashSet
import komplex.utils.findFilesInPath
import java.nio.file.Path
import komplex.utils.findGlobFiles

public trait DataCollection<Data: ArtifactData> : ArtifactData {
    public val coll: Iterable<Data>
}

public open class DataSet<Data: ArtifactData>(override val coll: HashSet<Data> = hashSetOf()) : DataCollection<Data> { }

public open class DataToDataCollectionAdaptor<Data: ArtifactData>(data: Data) : DataCollection<Data> {
    override val coll: Iterable<Data> = listOf(data)
}

public fun adaptToDataCollection<D: ArtifactData>(coll: DataCollection<D>) : DataCollection<D> = coll
public fun adaptToDataCollection<D: ArtifactData>(data: D) : DataCollection<D> = DataToDataCollectionAdaptor(data)


public fun openFileSet(vararg artifacts: komplex.dsl.FileArtifact): DataSet<FileData> =
    DataSet<FileData>( artifacts.map { SimpleFileData(it.path) }.toHashSet())

public fun openFileSet(vararg artifacts: komplex.dsl.FolderArtifact, baseDir: Path? = null): DataSet<FileData> =
    DataSet<FileData>( artifacts.flatMap { findFilesInPath(it.path, baseDir).map { SimpleFileData(it) } }.toHashSet())

public fun openFileSet(vararg artifacts: komplex.dsl.FileGlobArtifact, baseDir: Path? = null): DataSet<FileData> =
    DataSet<FileData>( artifacts.flatMap { findGlobFiles(it.included, it.excluded, baseDir).map { SimpleFileData(it) } }.toHashSet())

public fun openFileSet(vararg paths: Path): DataSet<FileData> =
    DataSet<FileData>( paths.map { SimpleFileData(it) }.toHashSet())

public fun openFileSet(artifact: DataSet<FileData>): DataSet<FileData> = artifact

public fun openFileSet(artifact: FileData): DataSet<FileData> = DataSet(hashSetOf(artifact))

public fun openFileSet(pair: Pair<komplex.model.ArtifactDesc, ArtifactData?>): DataCollection<FileData> {
    val snd = pair.second
    return when (snd) {
        is DataSet<*> -> if (snd.coll.isEmpty() || snd.coll.first() !is FileData) openFileSet(pair.first) else adaptToDataCollection(snd as FileData)
        is FileData -> adaptToDataCollection(snd)
        else -> openFileSet(pair.first)
    }
}

public fun openFileSet(vararg artifacts: komplex.model.ArtifactDesc): DataSet<FileData> {
    throw UnsupportedOperationException("cannot open $artifacts as FileSet")
}

public fun openFileSetI(paths: Iterable<Path>): DataSet<FileData> =
        DataSet<FileData>( paths.map { SimpleFileData(it) }.toHashSet())


// \todo consider implementing direct open functions for stream sets as for file sets

public fun openInputStreamSet(fileColl: DataCollection<FileData>): DataSet<InputStreamData> =
        DataSet<InputStreamData>( fileColl.coll.map { openInputStream(it) }.toHashSet())

public fun openOutputStreamSet(fileColl: DataCollection<FileData>): DataSet<OutputStreamData> =
        DataSet<OutputStreamData>( fileColl.coll.map { openOutputStream(it) }.toHashSet())

