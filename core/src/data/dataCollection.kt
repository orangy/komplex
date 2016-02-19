package komplex.data

import komplex.dsl.VariableArtifact
import komplex.model.ArtifactData
import java.security.MessageDigest
import java.util.Arrays
import java.util.Comparator
import java.util.HashSet

fun ByteArray.hashEquals(other: ByteArray): Boolean = Arrays.equals(this, other)

fun mergeHashes(artifacts: Iterable<ArtifactData?>): ByteArray {
    val digest = MessageDigest.getInstance("SHA-1")
    val comp = Comparator<komplex.model.ArtifactData?> { o1, o2 ->
        when {
            o1 == o2 -> 0 // same object or nulls
            o1 == null -> -1
            o2 == null -> 1
            o1.hash.size < o2.hash.size -> -1
            o1.hash.size > o2.hash.size -> 1
            else -> {
                val mismatch = o1.hash.asIterable().zip(o2.hash.asIterable()).firstOrNull { it.first != it.second }
                if (mismatch == null) 0
                else mismatch.first.compareTo(mismatch.second)
            }
        }
    }
    artifacts.sortedWith(comp).forEach { if (it != null) digest.update(it.hash) }
    return digest.digest()
}


interface DataCollection<Data: ArtifactData> : ArtifactData {
    val coll: Iterable<Data>
}

open class DataSet<Data: ArtifactData>(override val coll: HashSet<Data> = hashSetOf()) : DataCollection<Data> {
    override val hash: ByteArray by lazy { mergeHashes(coll)}
}

// second constructor
fun <Data: ArtifactData> DataSet(coll: Iterable<Data>): DataSet<Data> = DataSet(coll.toHashSet())


open class DataToDataCollectionAdaptor<Data: ArtifactData>(data: Data) : DataCollection<Data> {
    override val hash: ByteArray by lazy { mergeHashes(coll)}
    override val coll: Iterable<Data> = listOf(data)
}

fun <D: ArtifactData> adaptToDataCollection(coll: DataCollection<D>) : DataCollection<D> = coll
fun <D: ArtifactData> adaptToDataCollection(data: D) : DataCollection<D> = DataToDataCollectionAdaptor(data)


fun openDataSet(artifacts: Iterable<komplex.model.ArtifactDesc>): DataSet<ArtifactData> =
        DataSet( artifacts.flatMap {
            when (it) {
                is VariableArtifact<*> -> listOf(VariableData(it))
                else -> tryOpenFileSet(it)
                        ?: listOf(DummyData())
            }})

fun openDataSet(vararg artifacts: komplex.model.ArtifactDesc): DataSet<ArtifactData> = openDataSet(artifacts.asIterable())
