
package komplex.utils

import java.io.PrintStream


interface Named {
    val name: String
    // TODO: check usages and if needed find out how to overcome the limitation
//    override fun toString(): String = name
}

interface BuildDiagnostic {
    enum class Status {
        Succeeded,
        Failed
    }
    val status : Status
    val messages: Iterable<String> get() = listOf()
    companion object {
        val Success: BuildDiagnostic = object : BuildDiagnostic {
            override val status : BuildDiagnostic.Status = BuildDiagnostic.Status.Succeeded
        }
        fun Fail(msg: String): BuildDiagnostic = FailBuildDiagnostic(listOf(msg))
        fun Fail(vararg msgs: String): BuildDiagnostic = FailBuildDiagnostic(msgs.asIterable())
        fun Fail(msgs: Iterable<String>): BuildDiagnostic = FailBuildDiagnostic(msgs)
        val Fail : BuildDiagnostic = FailBuildDiagnostic()
    }
}


data class FailBuildDiagnostic(override val messages: Iterable<String> = listOf()) : BuildDiagnostic {
    override val status : BuildDiagnostic.Status = BuildDiagnostic.Status.Failed
}


operator fun BuildDiagnostic.plus(other: BuildDiagnostic): BuildDiagnostic =
        if (this.status == BuildDiagnostic.Status.Succeeded && other.status == BuildDiagnostic.Status.Succeeded)
            BuildDiagnostic.Success
        else
            BuildDiagnostic.Fail(this.messages + other.messages)


object spyConfig {
    var isOn: Boolean = true
    var stream: PrintStream = System.err
}

@Suppress("unused")
fun<T> spy(v: T, ctx: String? = null): T {
    if (spyConfig.isOn)
        spyConfig.stream.println("${if (ctx != null) "[$ctx] " else " "}$v")
    return v
}
