package komplex

enum class EventStyle {
    default
    reversed
}

open class Event(val name: String, val style: EventStyle = EventStyle.default) {
    val handlers: MutableList<(Project.(Event) -> Unit)> = arrayListOf()
    fun invoke(handler: Project.(Event) -> Unit) {
        handlers.add(handler)
    }

    fun fire(project: Project) {
        val process =
                when(style) {
                    EventStyle.default -> handlers
                    EventStyle.reversed -> handlers.reverse()
                }
        for (handler in process) {
            project.handler(this)
        }
    }
}