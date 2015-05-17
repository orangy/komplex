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
import komplex.tools.*
import komplex.tools.jar.addManifestProperty
import komplex.tools.javac.JavaCompilerRule
import komplex.tools.javac.javac
import komplex.tools.kotlin.KotlinCompilerRule
import komplex.tools.proguard.filters
import komplex.tools.proguard.options
import komplex.tools.proguard.proguard
import komplex.utils
import komplex.utils.Named
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

// \todo move into separate tool
class KotlinJavaToolRule(override val name: String, public val kotlin: KotlinCompilerRule = tools.kotlin, public val java: JavaCompilerRule = tools.javac)
: komplex.dsl.RuleSetDesc(listOf(kotlin, java)), Named {

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

    public fun<S> classpath(v: Iterable<S>): KotlinJavaToolRule { kotlin.classpath(v); java.classpath(v); return this }
    public fun<S> classpath(vararg v: S): KotlinJavaToolRule { kotlin.classpath(*v); java.classpath(*v); return this }
    public fun<S> classpath(vararg v: Iterable<S>): KotlinJavaToolRule { kotlin.classpath(*v); java.classpath(*v); return this }

    public fun with(body: KotlinJavaToolRule.() -> Unit): KotlinJavaToolRule {
        body()
        return this
    }
}

fun main(args: Array<String>) {

    // \todo detect root
    val rootDir = Paths.get(args.first())
    val javaHome = Paths.get(System.getenv("JAVA_HOME"),"jre") // as ant does
    val maxHeapSizeForForkedJvm = "1024m"

    val test = scenario("test")
    val jar = scenario("jar")
    val check = scenario("check")
    val publish = scenario("publish")

    val script = script {
        /// BUILD SCRIPT

        env.rootDir = rootDir

        val outputDir = rootDir / "out/kb"
        val libraries = folder(artifacts.binaries, outputDir / "libs")

        fun library(id: String, version: String? = null, scenario: Scenarios = Scenarios.Default_): Module {
            val libModule = komplex.tools.maven.mavenLibrary(id, version, target = libraries)
            libModule.build using tools.maven
            return libModule
        }

        val bootstrapHome = rootDir / "dependencies/bootstrap-compiler"
        val bootstrapCompilerHome = bootstrapHome / "Kotlin/kotlinc"
        val bootstrapRuntime = file(artifacts.jar, bootstrapHome / "Kotlin/lib/kotlin-runtime.jar")
        val bootstrapReflect = file(artifacts.jar, bootstrapHome / "Kotlin/lib/kotlin-reflect.jar")
        val bootstrapCompilerJar = file(artifacts.jar, bootstrapCompilerHome / "lib/kotlin-compiler.jar")
        val compilerJar = rootDir / "out/kb/artifacts/kotlin-compiler.jar"
        val bootstrapCompilerScript = bootstrapCompilerHome / "bin/kotlinc"

        val outputCompilerDir = outputDir / "kotlinc"
        val outputCompilerJar = file(artifacts.jar, outputCompilerDir / "lib/kotlin-compiler.jar")
        val outputBootstrapRuntime = file(artifacts.jar, outputCompilerDir / "lib/kotlin-runtime-internal-bootstrap.jar")
        val outputBootstrapReflect = file(artifacts.jar, outputCompilerDir / "lib/kotlin-reflect-internal-bootstrap.jar")

        fun bootstrapCompiler() = KotlinJavaToolRule("Boostrap compiler", kotlin = tools.kotlin(bootstrapCompilerJar.path))

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
                "js/js.serializer").map { folder(artifacts.sources, it) }


        module("kotlin") {

            version("ATTEMPT-0.1")

            depends on children

            val prepareDist = module("prepareDist") {
                build using(tools.copy) with {
                    from (folder(artifacts.binaries, "compiler/cli/bin"))
                    export (folder(artifacts.binaries, "out/kb/build.kt"))
                }
                build using(tools.copy) from bootstrapRuntime export outputBootstrapRuntime with { makeDirs = true }
                build using(tools.copy) from bootstrapReflect export outputBootstrapReflect with { makeDirs = true }
            }

            val protobufLite = module("protobufLite") {
                // choose the right one
                //val originalProtobuf = library("com.google.protobuf:protobuf-java:2.5.0")
                val originalProtobuf = file(artifacts.jar, "ideaSDK/lib/protobuf-2.5.0.jar")
                val protobufLite = file(artifacts.jar, libraries.path / "protobuf-2.5.0-lite.jar")

                fun runScript(srcs: Iterable<Pair<ArtifactDesc, ArtifactData?>>, tgts: Iterable<ArtifactDesc>): Iterable<ArtifactData> {
                    val target = tgts.first() as FileArtifact
                    target.path.getParent().toFile().mkdirs()

                    val res = run(bootstrapCompilerScript.toString(), "-script", escape4cli(rootDir / "generators/infrastructure/build-protobuf-lite.kts"),
                                escape4cli(srcs.getPaths().first()),
                                escape4cli(target.path))
                    if (res > 0)
                        throw Exception("Serializing builtins failed with error code $res")
                    return openFileSet(target).coll
                }

                build using(tools.custom(::runScript)) from originalProtobuf export protobufLite
            }

            val compiler = module("compiler", "Kotlin Compiler") {

//                depends.on (
//                        prepareDist,
//                        library("org.slf4j:slf4j-api:1.7.12"),
//                        library("org.jetbrains:annotations:13.0")
//                )
                // shared settings for all projects
                val jarFile = file(artifacts.jar, compilerJar)
                val checkedJarFile = file(artifacts.jar, "out/kb/artifacts/checked/kotlin-compiler.jar")
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

                val classes = build using bootstrapCompiler() with {
                    from(compilerSourceRoots)
                    classpath(libs)
                    kotlin.into(folder(artifacts.binaries, "out/kb/build/compiler.kt"))
                    java.into(folder(artifacts.binaries, "out/kb/build/compiler.java"))
                }

                val makeUncheckedJar = build(jar, test) using tools.jar with {
                    from(classes, jarContent)
                    into(jarFile)
                    dependsOn(prepareDist)
                    deflate = true
                    addManifestProperty("Main-Class", "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
                    addManifestProperty("Class-Path", listOf(outputBootstrapRuntime, outputBootstrapReflect).map { it.path.getFileName() }.joinToString(" "))
                }

                build(jar, test) using tools.copy from makeUncheckedJar export outputCompilerJar

                val makeCheckedJar = build(check) using tools.proguard from makeUncheckedJar into checkedJarFile with {
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

//                build(check) using tools.copy from makeCheckedJar export outputCompilerJar

                default(jar) // default build scenario, '*'/null if not specified (means - all)
            }

            fun newCompiler() = KotlinJavaToolRule("New compiler", kotlin =
                tools.kotlin(outputCompilerJar.path)) with {
                    kotlin.dependsOn(compiler)
                }

            val compilerSources = module("compiler-sources", "Kotlin Compiler sources") {
                val jarFile = file(artifacts.jar, outputDir / "artifacts/kotlin-compiler-sources.jar")
                build using tools.jar from compilerSourceRoots export jarFile with {
                    deflate = true
                }
            }

            val preloader = module("preloader", "Preloader") {

                val classes = folder(artifacts.binaries, outputDir / "build.kt/preloader")
                val sources = folder(artifacts.sources, "compiler/preloader/src")
                val preloaderJarFile = file(artifacts.jar, outputDir / "artifacts/kotlin-preloader.jar")

                build using(tools.javac) from sources into classes

                build using tools.jar from classes export preloaderJarFile with {
                    deflate = true
                }
            }

            val antTools = module("ant-tools", "Kotlin ant tools") {

                val antlib = library("org.apache.ant:ant:1.7.1")

                val classes = build using bootstrapCompiler() with {
                    from(listOf(folder(artifacts.sources, rootDir / "ant" )))
                    classpath( bootstrapRuntime,
                               antlib)
                    kotlin.into(folder(artifacts.binaries, "out/kb/build.kt/ant"))
                    java.into(folder(artifacts.binaries, "out/kb/build/ant"))
                }

                build using(tools.jar) with {
                    from(classes)
                    export(file(artifacts.jar, "out/kb/artifacts/kotlin-ant.jar"))
                    deflate = true
                }
            }


            val serializeBuiltins = module("serialize-builtins") {

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

                build using(tools.custom(::serialize)) with {
                    from( folder(artifacts.sources, "core/builtins/native"),
                          folder(artifacts.sources, "core/builtins/src"))
                    export(folder(artifacts.binaries, outputDir / "builtins"))
                }
            }


            val builtins = module("builtins") {

                val sources = listOf(
                    "core/builtins",
                    "core/runtime.jvm").map { folder(artifacts.sources, it) }

                build using newCompiler() with {
                    from(sources)
                    kotlin.export(folder(artifacts.binaries, "out/kb/build.ktnew/builtins.kt"))
                    java.export(folder(artifacts.binaries, "out/kb/build.ktnew/builtins.java"))
                    kotlin.enableInline = true
                    kotlin.includeRuntime = false
                }

            }

            val stdlib = module("stdlib") {

                build using tools.kotlin(outputCompilerJar.path) with {
                    from( files(artifacts.sources, "libraries/stdlib", "src/**.kt"))
                    classpath( builtins)
                    export( folder(artifacts.binaries, "out/kb/build.ktnew/stdlib.kt"))
                    enableInline = true
                    includeRuntime = false
                }
            }


            val core = module("core") {
                build using newCompiler() with {
                    kotlin.export (folder(artifacts.binaries, "out/kb/build.ktnew/core.kt"))
                    java.export (folder(artifacts.binaries, "out/kb/build.ktnew/core.java"))
                    from (listOf(
                        "core/descriptor.loader.java",
                        "core/descriptors",
                        "core/descriptors.runtime",
                        "core/deserialization",
                        "core/util.runtime").map { folder(artifacts.sources, it) })
                    classpath( builtins,
                               stdlib,
                               protobufLite,
                               file(artifacts.jar, "lib/javax.inject.jar"))
                }
            }

            val reflection = module("reflection") {
                build using newCompiler() with {
                    kotlin.export (folder(artifacts.binaries, "out/kb/build.ktnew/reflection.kt"))
                    java.export (folder(artifacts.binaries, "out/kb/build.ktnew/reflection.java"))
                    from (folder(artifacts.sources, "core/reflection.jvm"))
                    classpath( builtins,
                               stdlib,
                               core,
                               protobufLite)
                }
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

    //val scenarios = Scenarios.All
    val scenarios = scenarios(jar)
    val graph = script.buildGraph()

    println("\n--- roots -------------------------------")
    graph.roots(scenarios).forEach { println(it.nicePrint(indent, graph, scenarios)) }
    println("\n--- leafs -------------------------------")
    graph.leafs(scenarios).forEach { println(it.nicePrint(indent, graph, scenarios)) }
    println("\n--- plan --------------------------------")
    println(graph.nicePrint(indent, scenarios))

    println("\n--- build -------------------------------")
    graph.build(scenarios)

    println("\n-- done. --------------------------------")
}

