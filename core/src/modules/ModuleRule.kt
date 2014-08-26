package komplex

public abstract class ModuleRule(val parent: ModuleScenario) {
    public abstract fun execute(context: BuildContext): BuildResult

    open fun dump(indent: String = "") {

    }
}

public class ModuleToolRule<TTool : Tool>(parent: ModuleScenario, val tool: TTool) : ModuleRule(parent) {
    override fun dump(indent: String) {
        println("$indent Rule ${tool.title}")
        tool.dump(indent)
    }

    public override fun execute(context: BuildContext): BuildResult {
        try {
            return tool.execute(context)
        } catch (e: Throwable) {
            e.printStackTrace()
            return BuildResult.Fail
        }
    }
}
