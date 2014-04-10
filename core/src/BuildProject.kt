package komplex


class BuildProject(val project: Project) {
    val configurations = arrayListOf<BuildConfiguration>()

    fun using(tool: Tool): BuildStep = invoke(Config("*")).using(tool)

    fun invoke(vararg config: Config, body: BuildConfiguration.() -> Unit): BuildConfiguration {
        val buildConfiguration = invoke(*config)
        buildConfiguration.body()
        return buildConfiguration
    }

    fun invoke(vararg config: Config): BuildConfiguration {
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