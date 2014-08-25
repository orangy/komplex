package komplex


public class BuildModule(val module: Module) {
    val configurations = arrayListOf<BuildConfiguration>()

    public fun using(tool: Tool): BuildStep = invoke(Config("*")).using(tool)

    public fun invoke(vararg config: Config, body: BuildConfiguration.() -> Unit): BuildConfiguration {
        val buildConfiguration = invoke(*config)
        buildConfiguration.body()
        return buildConfiguration
    }

    public fun invoke(vararg config: Config): BuildConfiguration {
        val build = BuildConfiguration(this, config.toList())
        configurations.add(build)
        return build
    }

    fun dump(indent: String = "") {
        for (builds in configurations) {
            builds.dump(indent)
        }
    }
}