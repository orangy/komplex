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
        val Success = SingleBuildResult(BuildStatus.Succeeded)
        val Fail = SingleBuildResult(BuildStatus.Failed)
    }
}

public class SingleBuildResult(override val status : BuildStatus) : BuildResult {

}

public class MultipleBuildResult() : BuildResult {
    override val status: BuildStatus
        get() = if (results.all { it.status == BuildStatus.Succeeded }) BuildStatus.Succeeded else BuildStatus.Failed

    val results = arrayListOf<BuildResult>()

    fun append(result : BuildResult) = results.add(result)
}