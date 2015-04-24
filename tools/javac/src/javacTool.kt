package komplex.tools.javac

import java.lang
import java.io.*
import java.util.HashMap
import java.nio.file.Paths
import java.nio.file.Path
import org.slf4j.LoggerFactory
import kotlin.properties.Delegates
import komplex.model
import komplex.dsl
import komplex.tools
import komplex.data
import komplex.model.Tool
import komplex.model.ToolStep
import komplex.utils
import java.net.URI
import javax.tools.*


public val komplex.dsl.tools.javac: JavaCompilerRule
    get() = JavaCompilerRule(komplex.model.LazyTool<JavaCompilerRule, JavaCompiler>("Kotlin compiler", { JavaCompiler()} ))

val log = LoggerFactory.getLogger("komplex.tools.javac")

// separate class for separate class loading
// \todo check if moving to separate file or jar is needed for really lazy tool loading, or may be that nested class will work as well
public class JavaCompilerRule(override val tool: Tool<JavaCompilerRule>) : komplex.tools.CompilerRule<JavaCompilerRule, Tool<JavaCompilerRule>>() {
    override val name: String = "Java compiler"
    override val config: JavaCompilerRule = this

    public fun invoke(body: JavaCompilerRule.() -> Unit): JavaCompilerRule {
        body()
        return this
    }
    public var enableInline: Boolean = true
}


class JavaSource(path: String) : SimpleJavaFileObject(URI(path), JavaFileObject.Kind.SOURCE) {}

public class JavaCompiler() : komplex.model.Tool<JavaCompilerRule> {
    override val name: String = "Java compiler"

    //    override fun compile(context: BuildStepContext, from: Iterable<Artifact>, to: Iterable<Artifact>, useLibs: Iterable<Artifact>): BuildResult = null!!

    override fun execute(context: model.BuildContext,
                         cfg: JavaCompilerRule,
                         src: Iterable<Pair<model.ArtifactDesc, model.ArtifactData?>>,
                         tgt: Iterable<model.ArtifactDesc>
    ): model.BuildResult {
        val dependenciesSet = cfg.depSources.toHashSet()

        val libraries = src
                .filter { dependenciesSet.contains(it.first) }
                .flatMap { data.openFileSet(it).coll }
                .map { it.path.toAbsolutePath() }
                .distinct()
                // \todo convert to relative/optimal paths
                .joinToString(File.pathSeparator)

        val folder = tgt.single()
        val targetFolder =
                when (folder) {
                    is dsl.FolderArtifact -> folder.path.toString()
                    else -> throw IllegalArgumentException("Compiler only supports single folder as destination")
                }

        val options = arrayListOf("-cp", libraries, "-d", targetFolder)

        val compiler = ToolProvider.getSystemJavaCompiler()
        val diagnostics = DiagnosticCollector<JavaFileObject>()
        val fileManager=compiler.getStandardFileManager(diagnostics, null, null)

        val explicitSourcesSet = cfg.explicitSources.toHashSet()

        val sources = src.filter { explicitSourcesSet.contains(it.first) }
                .flatMap { data.openFileSet(it).coll.map { it.path.toString() }}

        val task=compiler.getTask(null,fileManager,diagnostics,options,null, sources.map { JavaSource(it) })

        log.info("javac: ${options.joinToString(" ")} ${sources.joinToString(" ")}")

        val success = task.call()

        for (diag in diagnostics.getDiagnostics()) {
            val msg = "${diag.getCode()} (${diag.getLineNumber()},${diag.getColumnNumber()}) ${diag.getMessage(null)}"
            when (diag.getKind()) {
                Diagnostic.Kind.ERROR -> log.error(msg)
                Diagnostic.Kind.NOTE -> log.debug(msg)
                Diagnostic.Kind.MANDATORY_WARNING -> log.info(msg)
                Diagnostic.Kind.WARNING -> log.info(msg)
            //Diagnostic.Kind.OTHER -> log.trace(msg)
            }
        }

        return if (success)
            model.BuildResult(utils.BuildDiagnostic.Success, listOf(Pair(folder, data.openFileSet(folder))))
            else model.BuildResult(utils.BuildDiagnostic.Fail)
    }
}
