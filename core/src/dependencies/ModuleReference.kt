package komplex

import java.util.ArrayList

public data class ModuleReference(val name: String, val version : String = "") {
    override fun toString(): String = "$name ($version)"
}

public class ModuleReferences() : MutableList<ModuleReference> by ArrayList<ModuleReference>()

public fun module(name: String): ModuleReference = ModuleReference(name)
public fun ModuleReferences(name : String): ModuleReferences = ModuleReferences().let { it.add(ModuleReference(name)); it }
public fun ModuleReferences(reference : ModuleReference): ModuleReferences = ModuleReferences().let { it.add(reference); it }


