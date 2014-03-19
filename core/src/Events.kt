package komplex

enum class EventStyle {
    default
    reversed
}

open class Event<T>(val name: String, val style: EventStyle = EventStyle.default) {
    val handlers: MutableList<(T.(Event<T>) -> Unit)> = arrayListOf()
    fun invoke(handler: T.(Event<T>) -> Unit) {
        handlers.add(handler)
    }

    fun fire(owner: T) {
        val process =
                when(style) {
                    EventStyle.default -> handlers
                    EventStyle.reversed -> handlers.reverse()
                }
        for (handler in process) {
            owner.handler(this)
        }
    }
}