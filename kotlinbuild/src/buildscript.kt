package kotlin.buildscript

import komplex.data.OpenFileSet
import komplex.data.openFileSet
import komplex.dsl.*
import komplex.dsl.tools
import komplex.dsl.Module
import komplex.tools.jar.jar
import komplex.tools.kotlin.kotlin
import komplex.tools.maven.maven
import komplex.model.*
import komplex.tools.copy
import komplex.tools.getPaths
import komplex.tools.javac.javac
import komplex.tools.proguard.filters
import komplex.tools.proguard.options
import komplex.tools.proguard.proguard
import komplex.tools.singleDestFolder
import komplex.tools.use
import komplex.utils
import komplex.utils.div
import komplex.utils.escape4cli
import komplex.utils.runProcess
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

internal val log = LoggerFactory.getLogger("kotlinbuild")

fun run(args: Iterable<String>): Int = runProcess(args, { log.debug(it) }, { log.error(it) })
fun run(vararg args: String): Int = runProcess(args.asIterable(), { log.debug(it) }, { log.error(it) })

fun main(args: Array<String>) {

    // \todo detect root
    val rootDir = Paths.get(args.first())
    val javaHome = Paths.get(System.getenv("JAVA_HOME"),"jre") // as ant does
    val maxHeapSizeForForkedJvm = "1024m"

    val script = script {
        /// BUILD SCRIPT

        env.rootDir = rootDir

        val test = scenario("test")
        val jar = scenario("jar")
        val publish = scenario("publish")

        val outputDir = rootDir.resolve("out/kb")
        val libraries = folder(artifacts.binaries, outputDir / "libs")

        fun library(id: String, version: String? = null, scenario: Scenarios = Scenarios.Default_): Module {
            val libModule = komplex.tools.maven.mavenLibrary(id, version, target = libraries)
            libModule.build using tools.maven
            return libModule
        }

        fun Module.shared(kotlinSources: Iterable<Artifact>, kotlinSourceRoots: Iterable<String>, javaSources: Iterable<Artifact>) {
            version("ATTEMPT-0.1")

            // shared settings for all projects
            val kotlinBinaries = folder(artifacts.binaries, "out/kb/build.kt/$moduleName")
            val javaBinaries = folder(artifacts.binaries, "out/kb/build/$moduleName")
            val jarFile = file(artifacts.jar, "out/kb/artifacts/kotlin-$moduleName-uncompressed.jar")
            val finalJarFile = file(artifacts.jar, "out/kb/artifacts/kotlin-$moduleName.jar")
            val bootstrapRuntime = file(artifacts.jar, "dependencies/bootstrap-compiler/Kotlin/lib/kotlin-runtime.jar")
            val libs = artifactsSet(
                    bootstrapRuntime,
                    file(artifacts.jar, "ideaSDK/lib/protobuf-2.5.0.jar"),
                    file(artifacts.jar, "dependencies/jline.jar"),
                    file(artifacts.jar, "dependencies/cli-parser-1.1.1.jar"),
                    file(artifacts.jar, "ideaSDK/jps/jps-model.jar"),
                    files(artifacts.jar, "ideaSDK/core", "*.jar"),
                    files(artifacts.jar, "lib", "*.jar"),
                    files(artifacts.jar, "lib", "**/*.jar")
            )
            val jarContent = artifactsSet(
                    files(artifacts.jar, "lib", "*.jar"),
                    files(artifacts.jar, "ideaSDK/core", "*.jar").exclude("util.jar"),
                    file(artifacts.jar, "ideaSDK/jps/jps-model.jar"),
                    file(artifacts.jar, "ideaSDK/lib/jna-utils.jar"),
                    file(artifacts.jar, "ideaSDK/lib/oromatcher.jar"),
                    file(artifacts.jar, "ideaSDK/lib/protobuf-2.5.0.jar"),
                    file(artifacts.jar, "dependencies/jline.jar"),
                    file(artifacts.jar, "dependencies/cli-parser-1.1.1.jar"),
                    files(artifacts.sources, "compiler/frontend.java/src", "META-INF/services/**"),
                    files(artifacts.sources, "compiler/backend/src", "META-INF/services/**"),
                    files(artifacts.sources, "resources", "kotlinManifest.properties"),
                    files(artifacts.sources, "idea/src", "META-INF/extensions/common.xml"),
                    files(artifacts.sources, "idea/src", "META-INF/extensions/kotlin2jvm.xml"),
                    files(artifacts.sources, "idea/src", "META-INF/extensions/kotlin2js.xml")
            )

            depends on children

            build using(tools.kotlin) from kotlinSources into kotlinBinaries with {
                use(depends.modules)
                use(libs)
                sourceRoots.addAll(kotlinSourceRoots)
                enableInline = true
            }

            build using(tools.javac) from javaSources into javaBinaries with {
                use(depends.modules)
                use(kotlinBinaries)
                use(libs)
            }

            build(jar, test) using tools.jar with {
                from(kotlinBinaries, javaBinaries, jarContent)
                export(jarFile)
                deflate = true
            }

            build(jar) using tools.proguard from jarFile export finalJarFile with {
                filters("!com/thoughtworks/xstream/converters/extended/ISO8601**",
                        "!com/thoughtworks/xstream/converters/reflection/CGLIBEnhancedConverter**",
                        "!com/thoughtworks/xstream/io/xml/Dom4J**",
                        "!com/thoughtworks/xstream/io/xml/Xom**",
                        "!com/thoughtworks/xstream/io/xml/Wstx**",
                        "!com/thoughtworks/xstream/io/xml/KXml2**",
                        "!com/thoughtworks/xstream/io/xml/BEAStax**",
                        "!com/thoughtworks/xstream/io/json/Jettison**",
                        "!com/thoughtworks/xstream/mapper/CGLIBMapper**",
                        "!org/apache/log4j/jmx/Agent*",
                        "!org/apache/log4j/net/JMS*",
                        "!org/apache/log4j/net/SMTP*",
                        "!org/apache/log4j/or/jms/MessageRenderer*",
                        "!org/jdom/xpath/Jaxen*",
                        "!org/mozilla/javascript/xml/impl/xmlbeans/**",
                        "!META-INF/maven**",
                        "**.class",
                        "**.properties",
                        "**.kt",
                        "**.kotlin_*",
                        "META-INF/services/**",
                        "META-INF/native/**",
                        "META-INF/extensions/**",
                        "META-INF/MANIFEST.MF",
                        "messages/**")
                options("""
                   -dontnote **
                   -dontwarn com.intellij.util.ui.IsRetina*
                   -dontwarn com.intellij.util.RetinaImage*
                   -dontwarn apple.awt.*
                   -dontwarn dk.brics.automaton.*
                   -dontwarn org.fusesource.**
                   -dontwarn org.xerial.snappy.SnappyBundleActivator
                   -dontwarn com.intellij.util.CompressionUtil
                   -dontwarn com.intellij.util.SnappyInitializer
                   -dontwarn net.sf.cglib.**
                   -dontwarn org.objectweb.asm.** # this is ASM3, the old version that we do not use
                   """)

                options("""
                   -libraryjars '${javaHome / "lib/rt.jar"}'
                   -libraryjars '${javaHome / "lib/jsse.jar"}'
                   -libraryjars '${bootstrapRuntime.path}'
                   """)

                options("""
                   -target 1.6
                   -dontoptimize
                   -dontobfuscate

                   -keep class org.fusesource.** { *; }
                   -keep class org.jdom.input.JAXPParserFactory { *; }

                   -keep class org.jetbrains.annotations.** {
                       public protected *;
                   }

                   -keep class javax.inject.** {
                       public protected *;
                   }

                   -keep class org.jetbrains.kotlin.** {
                       public protected *;
                   }

                   -keep class org.jetbrains.kotlin.compiler.plugin.** {
                       public protected *;
                   }

                   -keep class org.jetbrains.kotlin.extensions.** {
                       public protected *;
                   }

                   -keep class org.jetbrains.org.objectweb.asm.Opcodes { *; }

                   -keep class org.jetbrains.kotlin.codegen.extensions.** {
                       public protected *;
                   }

                   -keepclassmembers class com.intellij.openapi.vfs.VirtualFile {
                       public InputStream getInputStream();
                   }

                   -keep class jet.** {
                       public protected *;
                   }

                   -keep class com.intellij.psi.** {
                       public protected *;
                   }

                   -keep class com.intellij.openapi.util.TextRange { *; }
                   -keep class com.intellij.lang.impl.PsiBuilderImpl* {
                       public protected *;
                   }
                   -keep class com.intellij.openapi.util.text.StringHash { *; }

                   -keep class com.intellij.openapi.util.io.ZipFileCache { public *; }
                   -keep class com.intellij.openapi.util.LowMemoryWatcher { public *; }

                   -keepclassmembers enum * {
                       public static **[] values();
                       public static ** valueOf(java.lang.String);
                   }

                   -keepclassmembers class * {
                       ** toString();
                       ** hashCode();
                       void start();
                       void stop();
                       void dispose();
                   }

                   -keepclassmembers class org.jetbrains.org.objectweb.asm.Opcodes {
                       *** ASM5;
                   }

                   -keepclassmembers class org.jetbrains.org.objectweb.asm.ClassReader {
                       *** SKIP_CODE;
                       *** SKIP_DEBUG;
                       *** SKIP_FRAMES;
                   }
                   """
                )
            }

            default(jar) // default build scenario, '*'/null if not specified (means - all)
        }

        val bootstrapHome = rootDir / "dependencies/bootstrap-compiler"
        val bootstrapCompilerHome = bootstrapHome / "Kotlin/kotlinc"
        val compilerSourceRoots = listOf(
                "core/descriptor.loader.java",
                "core/descriptors",
                "core/deserialization",
                "core/util.runtime",
                "compiler/backend",
                "compiler/backend-common",
                "compiler/builtins-serializer",
                "compiler/cli",
                "compiler/cli/cli-common",
                "compiler/frontend",
                "compiler/frontend.java",
                "compiler/light-classes",
                "compiler/plugin-api",
                "compiler/serialization",
                "compiler/util",
                "js/js.dart-ast",
                "js/js.translator",
                "js/js.frontend",
                "js/js.inliner",
                "js/js.parser",
                "js/js.serializer")

        fun Module.makeFrom(baseDirs: Iterable<String>) =
                this.shared(
                        kotlinSources = baseDirs.map { files(artifacts.sources, it, "src/**.kt") },
                        kotlinSourceRoots = baseDirs.map { it + "/src" },
                        javaSources = baseDirs.map { files(artifacts.sources, it, "src/**.java") })
        fun Module.makeFrom(vararg baseDirs: String) = makeFrom(baseDirs.asIterable())

        module("kotlin") {

            val prepareDist = module("prepareDist") {

                val target = folder(artifacts.binaries, "out/kb/build.kt")
                val sources = folder(artifacts.binaries, "compiler/cli/bin")
                build using(tools.copy) from sources export target
            }

            val compiler = module("compiler", "Kotlin Compiler") {

                depends.on (
                        library("org.slf4j:slf4j-api:1.7.12"),
                        library("org.jetbrains:annotations:13.0")
                )
                makeFrom( compilerSourceRoots )
            }

            val preloader = module("preloader", "Preloader") {

                val classes = folder(artifacts.binaries, outputDir / "build.kt/preloader")
                val sources = folder(artifacts.sources, "compiler/preloader/src")
                val jarFile = file(artifacts.jar, outputDir / "artifacts/kotlin-preloader.jar")

                build using(tools.javac) from sources into classes

                build using tools.jar from classes export(jarFile) with {
                    deflate = true
                }
            }

            val serializeBuiltins = module("serialize-builtins") {

                val sources = artifactsSet(
                        folder(artifacts.sources, "core/builtins/native"),
                        folder(artifacts.sources, "core/builtins/src"))
                val target = folder(artifacts.binaries, outputDir / "builtins")

                fun serialize(srcs: Iterable<Pair<ArtifactDesc, ArtifactData?>>, tgts: Iterable<ArtifactDesc>): Iterable<ArtifactData> {
                    val classpath = depends.allArtifacts().getPaths(OpenFileSet.FoldersAsLibraries)
                    val target = tgts.singleDestFolder()

                    val params = arrayListOf( "java", "-Xmx$maxHeapSizeForForkedJvm")
                    if (classpath.any()) {
                        params.add("-cp")
                        params.add(escape4cli(classpath.joinToString(File.pathSeparator)))
                    }
                    params.addAll( listOf(
                            "-cp",
                            escape4cli(bootstrapCompilerHome / "lib/kotlin-compiler.jar"),
                            "org.jetbrains.kotlin.serialization.builtins.BuiltinsPackage",
                            escape4cli(target.path)) +
                            srcs.getPaths().map { escape4cli(it) })
                    val res = run(params)
                    if (res > 0)
                        throw Exception("Serializing builtins failed with error code $res")
                    return openFileSet(target).coll
                }

                build using(tools.custom(::serialize)) from sources export target
            }

            module("all", "Build All") {
                depends.on (
                        prepareDist,
                        preloader,
                        serializeBuiltins ,
                        compiler
                )
            }
        }
        /// BUILD SCRIPT
    }

    val indent = utils.TwoSpaceIndentLn()

    println("\n--- script ------------------------------")
    println(script.nicePrint(indent))

    val scenario = Scenarios.All
    val graph = script.buildGraph()

    println("\n--- roots -------------------------------")
    graph.roots(scenario).forEach { println(it.nicePrint(indent, graph, scenario)) }
    println("\n--- leafs -------------------------------")
    graph.leafs(scenario).forEach { println(it.nicePrint(indent, graph, scenario)) }
    println("\n--- plan --------------------------------")
    println(graph.nicePrint(indent, scenario))

    println("\n--- build -------------------------------")
    graph.build(scenario)

    println("\n-- done. --------------------------------")
}

