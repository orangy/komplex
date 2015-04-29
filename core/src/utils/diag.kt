
package komplex.utils

import java.io.PrintStream


public trait Named {
    public val name: String
    override fun toString(): String = name
}

public trait BuildDiagnostic {
    public enum class Status {
        Succeeded
        Failed
    }
    public val status : Status
    public val message : String get() = ""
    companion object {
        public val Success: BuildDiagnostic = object : BuildDiagnostic {
            override val status : BuildDiagnostic.Status = BuildDiagnostic.Status.Succeeded
        }
        public fun Fail(msg: String): BuildDiagnostic = FailBuildDiagnostic(msg)
        public val Fail : BuildDiagnostic = FailBuildDiagnostic("")
    }
}


public class FailBuildDiagnostic(override val message : String) : BuildDiagnostic {
    override val status : BuildDiagnostic.Status = BuildDiagnostic.Status.Failed
}



public object spyConfig {
    public var isOn: Boolean = true
    public var stream: PrintStream = System.err
}

public fun<T> spy(v: T, ctx: String? = null): T {
    if (spyConfig.isOn)
        spyConfig.stream.println("${if (ctx != null) "[$ctx] " else " "}$v")
    return v
}
