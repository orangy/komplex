package komplex.tools.kotlin

import komplex.dsl.*
import komplex.tools.classpath
import komplex.tools.javac.javac
import komplex.tools.kotlin.KotlinCompilerRule
import komplex.tools.kotlin.kotlin
import komplex.utils.Named
import komplex.utils.div

// \todo move into separate tool
class KotlinJavaToolRule(override val name: String, public val kotlin: KotlinCompilerRule/* = tools.kotlin*/, public val java: komplex.tools.javac.JavaCompilerRule = tools.javac)
: RuleSetDesc(listOf(kotlin, java)), Named {

    // \todo support skip property (as in javac2)

    init { java.classpath(kotlin) }

    public fun from(sourceRootDirs: Iterable<FolderArtifact>): KotlinJavaToolRule {
        kotlin.with {
            sourceRoots.addAll( sourceRootDirs.map { (it.path / "src").toString() })
            from(sourceRootDirs.map { files(artifacts.sources, it.path, "src/**.kt") })
        }
        java.with {
            from(sourceRootDirs.map { files(artifacts.sources, it.path, "src/**.java") })
        }
        return this
    }
    public fun from(vararg sourceRootDirs: FolderArtifact): KotlinJavaToolRule = from(sourceRootDirs.asIterable())

    public fun<S: GenericSourceType> classpath(v: Iterable<S>): KotlinJavaToolRule { kotlin.classpath(v); java.classpath(v); return this }
    public fun<S: GenericSourceType> classpath(vararg v: S): KotlinJavaToolRule { kotlin.classpath(*v); java.classpath(*v); return this }

    public fun with(body: KotlinJavaToolRule.() -> Unit): KotlinJavaToolRule {
        body()
        return this
    }
}