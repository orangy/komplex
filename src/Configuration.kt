package komplex

fun Project.configuration(name : String, body : Configuration.()->Unit) : Configuration {
    val configuration = Configuration(name, this)
    configuration.body()
    return configuration
}

class Configuration(val name : String, project : Project) {
    
}