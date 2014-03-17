package komplex

class Files {
    val included = arrayListOf<Path>()
    val excluded = arrayListOf<Path>()

    fun invoke(body: Files.() -> Unit) {
        this.body()
    }

    fun append(files : Files) {
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

    fun dump(indent: String = "") {
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