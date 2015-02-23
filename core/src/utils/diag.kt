
package komplex.utils


public trait Named {
    public val name: String
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
