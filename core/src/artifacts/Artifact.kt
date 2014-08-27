package komplex

public trait Artifact {
    val `type`: ArtifactType
}

public trait ArtifactType {
}

public class NamedArtifactType(val name: String) : ArtifactType {
    override fun toString(): String = "($name)"
}

public object artifacts {
    public val sources: ArtifactType = NamedArtifactType("src")
    public val binaries: ArtifactType = NamedArtifactType("bin")
    public val jar: ArtifactType = NamedArtifactType("jar")
}