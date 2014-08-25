package komplex

enum class BuildStatus {
    Succeeded
    Failed
}

val BuildResult.failed : Boolean
    get() = status == BuildStatus.Failed

val BuildResult.succeeded : Boolean
    get() = status == BuildStatus.Succeeded

public trait BuildResult {
    val status : BuildStatus
    class object {
        val Success = StepBuildResult(BuildStatus.Succeeded)
        val Fail = StepBuildResult(BuildStatus.Failed)
    }
}

public class StepBuildResult(override val status : BuildStatus) : BuildResult {

}

public class ModuleBuildResult() : BuildResult {
    override val status: BuildStatus
        get() = if (results.all { it.status == BuildStatus.Succeeded }) BuildStatus.Succeeded else BuildStatus.Failed

    val results = arrayListOf<BuildResult>()

    fun append(result : BuildResult) = results.add(result)
}