
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
    public val messages: Iterable<String> get() = listOf()
    companion object {
        public val Success: BuildDiagnostic = object : BuildDiagnostic {
            override val status : BuildDiagnostic.Status = BuildDiagnostic.Status.Succeeded
        }
        public fun Fail(msg: String): BuildDiagnostic = FailBuildDiagnostic(listOf(msg))
        public fun Fail(vararg msgs: String): BuildDiagnostic = FailBuildDiagnostic(msgs.asIterable())
        public fun Fail(msgs: Iterable<String>): BuildDiagnostic = FailBuildDiagnostic(msgs)
        public val Fail : BuildDiagnostic = FailBuildDiagnostic()
    }
}


public class FailBuildDiagnostic(override val messages: Iterable<String> = listOf()) : BuildDiagnostic {
    override val status : BuildDiagnostic.Status = BuildDiagnostic.Status.Failed
}


public fun BuildDiagnostic.plus(other: BuildDiagnostic): BuildDiagnostic =
        if (this.status == BuildDiagnostic.Status.Succeeded && other.status == BuildDiagnostic.Status.Succeeded)
            BuildDiagnostic.Success
        else
            BuildDiagnostic.Fail(this.messages + other.messages)


public object spyConfig {
    public var isOn: Boolean = true
    public var stream: PrintStream = System.err
}

public fun<T> spy(v: T, ctx: String? = null): T {
    if (spyConfig.isOn)
        spyConfig.stream.println("${if (ctx != null) "[$ctx] " else " "}$v")
    return v
}
