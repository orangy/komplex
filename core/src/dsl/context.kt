package komplex.dsl

import java.nio.file.Path

public interface ScriptContext {
    val env: ContextEnvironment
}

class ContextVarDelegate<T: Any>(val prepare: ((T) -> T)? = null) {
    private var v: T? = null
    fun get(thisRef: Any?, prop: PropertyMetadata): T? {

        val thisRefCtx = thisRef as ContextEnvironment
        if (v != null) return v
        val parent = thisRefCtx.parentContext?.env
        if (parent != null) {
            val value = ContextEnvironment::class.properties.first { it.name == prop.name }.get(parent)
            if (value != null) return value as? T? ?: throw Exception("Invalid property $prop = $value")
        }
        return null
    }

    fun set(thisRef: Any?, prop: PropertyMetadata, value: T?) {
        v = if (value != null && prepare != null) prepare!!(value) else value
    }
}


public class ContextEnvironment(val parentContext: ScriptContext?) {
    public var rootDir: Path? by ContextVarDelegate<Path>({ it.toAbsolutePath().normalize() })
    public var defaultTargetDir: Path? by ContextVarDelegate<Path>({ it.toAbsolutePath().normalize() })
    public var tempDir: Path? by ContextVarDelegate<Path>({ it.toAbsolutePath().normalize() })
}
