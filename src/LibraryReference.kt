package komplex

import java.util.ArrayList

fun LibraryReferences(library : LibraryReference) = LibraryReferences().let { it.add(library); it }
class LibraryReferences : MutableList<LibraryReference> by ArrayList<LibraryReference>()

fun LibraryReferences.library(name : String, version : String = "", pkg : String = "") : LibraryReference {
    val library = LibraryReference(name, version, pkg)
    add(library)
    return library
}

class LibraryReference(val name : String, val version : String, val pkg : String) {
    val fullName : String
    get() = listOf(name, version, pkg).filter { it.length > 0 }.makeString(", ")
}

fun Project.library(name : String, version : String = "", pkg : String = "") : LibraryReference {
    val library = LibraryReference(name, version, pkg)
    return library
}

fun Project.libraries(body : LibraryReferences.()->Unit) : LibraryReferences {
    val collection = LibraryReferences()
    collection.body()
    return collection
}

