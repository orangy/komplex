package komplex.tools.javascript

import com.google.javascript.jscomp.CompilationLevel
import com.google.javascript.jscomp.Compiler
import com.google.javascript.jscomp.CompilerOptions
import com.google.javascript.jscomp.SourceFile
import komplex.data.openFileSet
import komplex.data.openInputStream
import komplex.data.openOutputStream
import komplex.dsl.*
import komplex.model.ArtifactDesc
import komplex.model.Tool
import komplex.tools.configureSingleTempFileTarget
import komplex.tools.filterIn
import komplex.tools.getPaths
import komplex.utils.BuildDiagnostic
import komplex.utils.LogLevel
import komplex.utils.LogOutputStream
import komplex.utils.plus
import org.slf4j.LoggerFactory
import java.io.PrintStream
import java.nio.charset.StandardCharsets

val log = LoggerFactory.getLogger("komplex.tools.js")

val komplex.dsl.tools.closureCompiler: ClosureCompilerRule
    get() = ClosureCompilerRule(komplex.model.LazyTool<ClosureCompilerRule, ClosureCompilerTool>("closure javascript compiler", { ClosureCompilerTool() } ))

class ClosureCompilerRule(tool: Tool<ClosureCompilerRule>) : komplex.dsl.BasicToolRule<ClosureCompilerRule, komplex.model.Tool<ClosureCompilerRule>>(tool) {

    val explicitExterns: RuleSources = RuleSources()

    val externSources: Iterable<ArtifactDesc> get() = explicitExterns.collect(selector.scenarios)

    override val sources: Iterable<ArtifactDesc> get() = super.sources + externSources

    override fun configure(): BuildDiagnostic =
            super.configure() +
                    configureSingleTempFileTarget(module, artifacts.jar, { "${module.name}.${name.replace(' ','_')}.js" })
}

fun <S: GenericSourceType> ClosureCompilerRule.extern(args: Iterable<S>): ClosureCompilerRule = addToSources(explicitExterns, args)
fun <S: GenericSourceType> ClosureCompilerRule.extern(vararg args: S): ClosureCompilerRule = addToSources(explicitExterns, *args)


class ClosureCompilerTool() : komplex.model.Tool<ClosureCompilerRule> {

    override val name: String = "closure javascript compiler"

    override fun execute(context: komplex.model.BuildContext,
                         cfg: ClosureCompilerRule,
                         src: Iterable<Pair<komplex.model.ArtifactDesc, komplex.model.ArtifactData?>>,
                         tgt: Iterable<komplex.model.ArtifactDesc>
    ): komplex.model.BuildResult {

        val project = context.module

        val kotlinSources = src.filterIn(cfg.fromSources).getPaths()

        if (kotlinSources.none()) {
            log.error("Error: No sources to compile in module ${project.name}: ${src.map { it.first }}")
            return komplex.model.BuildResult(komplex.utils.BuildDiagnostic.Fail)
        }

        // first is target second is meta
        val dest = tgt.single() as? FileArtifact ?: throw Exception("Expecting file artifact as a target for tool $name")

// \todo implement it and reuse for error handling
//        val errorManager = object : ErrorManager { }

        val compiler = Compiler(PrintStream(LogOutputStream(log, LogLevel.INFO)))
        val options = CompilerOptions()
        CompilationLevel.WHITESPACE_ONLY.setOptionsForCompilationLevel(options)
        options.languageIn = CompilerOptions.LanguageMode.ECMASCRIPT5_STRICT

        fun Iterable<Pair<komplex.model.ArtifactDesc, komplex.model.ArtifactData?>>.openSourceStreams(): List<SourceFile> =
            flatMap { openFileSet(it).coll }.map { SourceFile.fromInputStream(it.path.toString(), openInputStream(it).inputStream, StandardCharsets.UTF_8) }

        val res = compiler.compile(
                src.filterIn(cfg.externSources).openSourceStreams(),
                src.filterIn(cfg.fromSources).openSourceStreams(),
                options)

        if (res.success) {
            val streamData = openOutputStream(dest)
            streamData.outputStream.writer().write(compiler.toSource())
            return komplex.model.BuildResult(komplex.utils.BuildDiagnostic.Success, listOf(Pair(dest, streamData)))
        }
        else return komplex.model.BuildResult(komplex.utils.BuildDiagnostic.Fail(res.errors.map { "Error in '${it.sourceName}' (${it.lineNumber}): ${it.description}" }))
    }
}
