package komplex.dsl

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KProperty
import kotlin.reflect.memberProperties

interface ScriptContext {
    val env: ContextEnvironment
}

class ContextVarDelegate<T: Any?>(val prepare: ((T) -> T)? = null, default_: T? = null) {
    private var v: T? = default_
    private var isSet: Boolean = false
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): T? {

        val thisRefCtx = thisRef as ContextEnvironment
        val parent = thisRefCtx.parentContext?.env
        if (isSet || parent == null) return v
        else {
            val value = ContextEnvironment::class.let { it.memberProperties }.first { it.name == prop.name }.get(parent)
            if (value != null && value as? T == null) throw IllegalArgumentException("Invalid property $prop = $value")
            return value as T
        }
    }

    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: T) {
        v = if (value != null && prepare != null) prepare.invoke(value) else value
    }
}


class ContextEnvironment(val parentContext: ScriptContext?) {
    var rootDir: Path? by ContextVarDelegate<Path?>({ it?.toAbsolutePath()?.normalize() })
    var defaultTargetDir: Path? by ContextVarDelegate<Path?>({ it?.toAbsolutePath()?.normalize() })
    var tempDir: Path? by ContextVarDelegate<Path?>({ it?.toAbsolutePath()?.normalize() })
    var libDir: Path? by ContextVarDelegate({ it.toAbsolutePath().normalize() }, Paths.get(".","libs"))
}
