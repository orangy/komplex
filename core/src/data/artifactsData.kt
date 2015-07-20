package komplex.data

import komplex.dsl.VariableArtifact
import java.nio.ByteBuffer


public class DummyData : komplex.model.ArtifactData {
    override val hash: ByteArray = ByteArray(0)
}

public class VariableData<T: Any>(val variable: VariableArtifact<T>) : komplex.model.ArtifactData {
    override val hash: ByteArray by lazy { ByteBuffer.allocate(4).putInt(variable.ref.hashCode()).array() }
}

