package komplex

import java.util.ArrayList

public class Path(val value: String) {
}

class PathCollection(val collection : MutableList<Path>)  {
    fun path(value : String) {
        collection.add(Path(value))
    }
}