
package komplex.utils

import java.io.PrintStream


public trait Named {
    public val name: String
    override fun toString(): String = name
}

// \todo add exception and/or message
public trait BuildDiagnostic {
    public enum class Status {
        Succeeded
        Failed
    }
    public val status : Status
    class object {
        public val Success: StepBuildDiagnostic = StepBuildDiagnostic(Status.Succeeded)
        public val Fail: StepBuildDiagnostic = StepBuildDiagnostic(Status.Failed)
    }
}


public class StepBuildDiagnostic(override public val status : BuildDiagnostic.Status) : BuildDiagnostic {}


public object spyConfig {
    public var isOn: Boolean = true
    public var stream: PrintStream = System.err
}

public fun<T> spy(v: T, ctx: String? = null): T {
    if (spyConfig.isOn)
        spyConfig.stream.println("${if (ctx != null) "[$ctx] " else " "}$v")
    return v
}
