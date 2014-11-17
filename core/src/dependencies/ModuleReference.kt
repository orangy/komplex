package komplex

import java.util.ArrayList

public trait Reference {
    public val name: String
    public override fun toString(): String = name
}

public class LibraryReference(public override val name: String, public val version: String? = null) : Reference {
    public fun baseName(): String = if (version == null) name else "$name-$version"
    public override fun toString(): String = baseName()
}

public fun library(name: String, version: String? = null): Reference = LibraryReference(name, version)

public class ModuleReference(public val module: Module) : Reference {
    override val name: String = module.moduleName
}

public class References() : MutableList<Reference> by ArrayList<Reference>() {
    public fun library(name: String, version: String? = null): Unit { add(LibraryReference(name, version)) }
    public fun module(module: Module): Unit { add(ModuleReference(module)) }
}

public fun References(reference : Reference): References = References().let { it.add(reference); it }


