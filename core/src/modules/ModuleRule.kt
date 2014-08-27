package komplex

public abstract class ModuleRule(val parent: ModuleScenario) {
    public abstract fun execute(context: BuildContext): BuildResult
}

public class ModuleToolRule<TTool : Tool>(parent: ModuleScenario, val tool: TTool) : ModuleRule(parent) {
    public override fun execute(context: BuildContext): BuildResult {
        try {
            return tool.execute(context)
        } catch (e: Throwable) {
            e.printStackTrace()
            return BuildResult.Fail
        }
    }
}
