package komplex

import java.util.ArrayList

public class Path(val value: String) {
    override fun toString(): String {
        return value
    }
}

class PathCollection(val collection: MutableList<Path>) {
    fun path(value: String) {
        collection.add(Path(value))
    }
}
