package komplex

fun file(path: String) = File(Path(path))
fun files(path: String) = FileSet().let { it.include(path); it }
fun folder(path: String) = Folder(Path(path))

class Folder(val path : Path) : BuildEndPoint {
    override fun dump(indent: String) {
        println("$indent Folder ${path}")
    }
}

class File(val path : Path) : BuildEndPoint {
    override fun dump(indent: String) {
        println("$indent File ${path}")
    }
}

class FileSet : BuildEndPoint {
    val included = arrayListOf<Path>()
    val excluded = arrayListOf<Path>()

    override fun toString(): String {
        if (excluded.isEmpty())
            return "$included"
        else
            return "$included - $excluded"
    }

    fun invoke(body: FileSet.() -> Unit) {
        this.body()
    }

    fun append(files : FileSet) {
        included.addAll(files.included)
        excluded.addAll(files.excluded)
    }

    fun include(path : String) {
        included.add(Path(path))
    }

    fun exclude(path : String) {
        excluded.add(Path(path))
    }

    fun include(body: PathCollection.() -> Unit) {
        val collection = PathCollection(included)
        collection.body()
    }

    fun exclude(body: PathCollection.() -> Unit) {
        val collection = PathCollection(excluded)
        collection.body()
    }

    override fun dump(indent: String) {
        if (included.isNotEmpty()) {
            for (child in included) {
                println("$indent ${child.value}")
            }
        }
        if (excluded.isNotEmpty()) {
            println("$indent Excluded:")
            for (child in excluded) {
                println("$indent   ${child.value}")
            }
        }
    }
}