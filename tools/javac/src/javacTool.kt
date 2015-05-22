package komplex.tools.javac

import java.lang
import java.io.*
import java.nio.file.Paths
import java.nio.file.Path
import org.slf4j.LoggerFactory
import kotlin.properties.Delegates
import komplex.model
import komplex.dsl
import komplex.tools
import komplex.data
import komplex.data.OpenFileSet
import komplex.data.openFileSet
import komplex.model.ArtifactData
import komplex.model.ArtifactDesc
import komplex.model.Tool
import komplex.model.ToolStep
import komplex.tools.filterIn
import komplex.tools.getPaths
import komplex.utils
import komplex.utils.escape4cli
import java.net.URI
import java.nio.channels.FileChannel
import java.util.*
import javax.tools.*


public val komplex.dsl.tools.javac: JavaCompilerRule
    get() = JavaCompilerRule(komplex.model.LazyTool<JavaCompilerRule, JavaCompiler>("Kotlin compiler", { JavaCompiler()} ))

val log = LoggerFactory.getLogger("komplex.tools.javac")

// separate class for separate class loading
// \todo check if moving to separate file or jar is needed for really lazy tool loading, or may be that nested class will work as well
public class JavaCompilerRule(override val tool: Tool<JavaCompilerRule>) : komplex.tools.JVMCompilerRule<JavaCompilerRule, Tool<JavaCompilerRule>>() {
    override val name: String = "Java compiler"
    override val config: JavaCompilerRule = this

    public fun invoke(body: JavaCompilerRule.() -> Unit): JavaCompilerRule {
        body()
        return this
    }
    public var enableInline: Boolean = true
}


fun JavaSource(path: String) = JavaSource(File(path))

class JavaSource(val file: File) : SimpleJavaFileObject(file.toURI(), JavaFileObject.Kind.SOURCE) {
    override fun openInputStream(): InputStream? = file.inputStream()

    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence? =
        openInputStream()!!.bufferedReader().readText()
}

public class JavaCompiler() : komplex.model.Tool<JavaCompilerRule> {
    override val name: String = "Java compiler"

    //    override fun compile(context: BuildStepContext, from: Iterable<Artifact>, to: Iterable<Artifact>, useLibs: Iterable<Artifact>): BuildResult = null!!

    override fun execute(context: model.BuildContext,
                         cfg: JavaCompilerRule,
                         src: Iterable<Pair<model.ArtifactDesc, model.ArtifactData?>>,
                         tgt: Iterable<model.ArtifactDesc>
    ): model.BuildResult {
        val libraries = src.filterIn(cfg.classpathSources).getPaths(OpenFileSet.FoldersAsLibraries).distinct()

        val folder = tgt.single()
        val targetFolder =
                when (folder) {
                    is dsl.FolderArtifact -> {
                        val dir = folder.path.toFile()
                        if (!dir.exists())
                            dir.mkdirs()
                        folder.path.toString()
                    }
                    else -> throw IllegalArgumentException("Compiler only supports single folder as destination")
                }

        val options = arrayListOf("-cp", libraries.joinToString(File.pathSeparator), "-d", targetFolder)

        val compiler = ToolProvider.getSystemJavaCompiler()
        val diagnostics = DiagnosticCollector<JavaFileObject>()
        val fileManager=compiler.getStandardFileManager(diagnostics, null, null)

        val sources = src.filterIn(cfg.fromSources).getPaths()

        log.debug("javac options: ${options.joinToString(" ")}")
        log.debug("sources: ${sources.map { escape4cli(it) }.joinToString("\n  ","\n  ")}")

        val task=compiler.getTask(null,fileManager,diagnostics,options,null, sources.map { JavaSource(it.toFile()) })

        log.info("start compilation")

        val success = task.call()

        log.info("compilation finished. diagnostics:")

        for (diag in diagnostics.getDiagnostics()) {
            val msg = "${diag.getSource()?.getName() ?: "<no source>"} (${diag.getLineNumber()},${diag.getColumnNumber()}) ${diag.getMessage(null)}"
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

