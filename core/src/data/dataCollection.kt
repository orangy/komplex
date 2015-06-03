package komplex.data

import komplex.dsl.VariableArtifact
import komplex.model.ArtifactData
import java.security.MessageDigest
import java.util.Arrays
import java.util.Comparator
import java.util.HashSet
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


public interface DataCollection<Data: ArtifactData> : ArtifactData {
    public val coll: Iterable<Data>
}

public open class DataSet<Data: ArtifactData>(override val coll: HashSet<Data> = hashSetOf()) : DataCollection<Data> {
    override val hash: ByteArray by Delegates.lazy { mergeHashes(coll)}
}

// second constructor
public fun DataSet<Data: ArtifactData>(coll: Iterable<Data>): DataSet<Data> = DataSet(coll.toHashSet())


public open class DataToDataCollectionAdaptor<Data: ArtifactData>(data: Data) : DataCollection<Data> {
    override val hash: ByteArray by Delegates.lazy { mergeHashes(coll)}
    override val coll: Iterable<Data> = listOf(data)
}

public fun adaptToDataCollection<D: ArtifactData>(coll: DataCollection<D>) : DataCollection<D> = coll
public fun adaptToDataCollection<D: ArtifactData>(data: D) : DataCollection<D> = DataToDataCollectionAdaptor(data)


public fun openDataSet(artifacts: Iterable<komplex.model.ArtifactDesc>): DataSet<ArtifactData> =
        DataSet( artifacts.flatMap {
            when (it) {
                is VariableArtifact<*> -> listOf(VariableData(it))
                else -> tryOpenFileSet(it)
                        ?: listOf(DummyData())
            }})

public fun openDataSet(vararg artifacts: komplex.model.ArtifactDesc): DataSet<ArtifactData> = openDataSet(artifacts.asIterable())
