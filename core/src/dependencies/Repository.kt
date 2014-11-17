
package komplex.dependencies

import java.nio.file.Path
import komplex.ModuleReference
import komplex.Artifact
import java.nio.file.Paths
import java.nio.file.Files
import java.util.ArrayList
import komplex.LibraryReference

public trait Repository {
    val name: String
    public fun resolve(reference: LibraryReference) : List<Path>
}

public open class LocalLibDirectory(path: Path) : Repository {
    val path: Path = path
    override val name: String = path.toString()
    override fun resolve(reference: LibraryReference) : List<Path> {
        val filePath = Paths.get(path.toString(), reference.baseName() + ".jar")
        return if (filePath != null && Files.exists(filePath)) listOf(filePath) else listOf();
    }
}

public fun repository(path : Path) : Repository {
    val res = LocalLibDirectory(path)
    repositories.list.add(res)
    return res;
}

public fun repository(relPath : String) : Repository {
    return repository(Paths.get(".", relPath)!!.toAbsolutePath())
}

public object repositories {
    public val list : MutableList<Repository> = ArrayList()
    public fun resolve(reference: LibraryReference) : List<Path> {
        val it = list.iterator()
        while (it.hasNext()) {
            val res: List<Path> = it.next().resolve(reference)
            if (!res.empty) return res
        }
        return listOf()
    }
}
