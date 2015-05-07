package komplex.dsl

import java.nio.file.Path

public trait ScriptContext {
    val env: ContextEnvironment
}

public class ContextEnvironment(val parentContext: ScriptContext?) {

    // \todo write generic property for that
    private var rootDirImpl: Path? = null
    public var rootDir: Path?
        get() = rootDirImpl ?: parentContext?.env?.rootDir
        set(v: Path?) { rootDirImpl = v }
}

