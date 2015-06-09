package komplex.dsl

import java.nio.file.Path
import java.nio.file.Paths

public interface ScriptContext {
    val env: ContextEnvironment
}

class ContextVarDelegate<T: Any?>(val prepare: ((T) -> T)? = null, default_: T = null) {
    private var v: T = default_
    private var isSet: Boolean = false
    fun get(thisRef: Any?, prop: PropertyMetadata): T {

        val thisRefCtx = thisRef as ContextEnvironment
        val parent = thisRefCtx.parentContext?.env
        if (isSet || parent == null) return v
        else {
            val value = ContextEnvironment::class.properties.first { it.name == prop.name }.get(parent)
            if (value != null && value as? T == null) throw IllegalArgumentException("Invalid property $prop = $value")
            return value as T
        }
    }

    fun set(thisRef: Any?, prop: PropertyMetadata, value: T) {
        v = if (value != null && prepare != null) prepare!!(value) else value
    }
}


public class ContextEnvironment(val parentContext: ScriptContext?) {
    public var rootDir: Path? by ContextVarDelegate<Path?>({ it?.toAbsolutePath()?.normalize() })
    public var defaultTargetDir: Path? by ContextVarDelegate<Path?>({ it?.toAbsolutePath()?.normalize() })
    public var tempDir: Path? by ContextVarDelegate<Path?>({ it?.toAbsolutePath()?.normalize() })
    public var libDir: Path by ContextVarDelegate({ it.toAbsolutePath().normalize() }, Paths.get(".","libs"))
}
