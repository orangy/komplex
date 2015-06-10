package kotlin.buildscript

import komplex.data.OpenFileSet
import komplex.data.VariableData
import komplex.data.openFileSet
import komplex.data.openInputStream
import komplex.dsl.*
import komplex.dsl.Module
import komplex.model.*
import komplex.tools.*
import komplex.tools.jar.addManifestProperty
import komplex.tools.jar.from
import komplex.tools.jar.jar
import komplex.tools.javac.javac
import komplex.tools.javascript.closureCompiler
import komplex.tools.javascript.extern
import komplex.tools.kotlin.KotlinJavaToolRule
import komplex.tools.kotlin.kotlin
import komplex.tools.kotlin.kotlinjs
import komplex.tools.kotlin.meta
import komplex.tools.maven.maven
import komplex.tools.proguard.filters
import komplex.tools.proguard.options
import komplex.tools.proguard.proguard
import komplex.utils
import komplex.utils.escape4cli
import komplex.utils.runProcess
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.file.Paths

internal val log = LoggerFactory.getLogger("kotlinbuild")


fun run(args: Iterable<String>): Int = runProcess(args, { log.debug(it) }, { log.error(it) })
fun run(vararg args: String): Int = runProcess(args.asIterable(), { log.debug(it) }, { log.error(it) })

val Module.copy: CopyToolRule get() = build using(tools.copy)

fun Module.library(id: String, version: String? = null, scenario: Scenarios = Scenarios.Default_): Module {
    val libModule = komplex.tools.maven.mavenLibrary(id, version, target = folder % env.libDir.jar)
    libModule.build using tools.maven
    this.children.add(libModule)
    return libModule
}

fun transformTargets<T: GenericSourceType>(source: T, fn: (ArtifactDesc) -> Artifact ): komplex.dsl.LambdaRule  {
    // \todo find more generic and safe way of implementing artifact transformation tools
    return tools.custom { srcs, tgts -> openFileSet(tgts).coll } with {
        from (source)
        fromSources.forEach { into (fn(it)) }
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

        val bootstrapHome = rootDir / "dependencies/bootstrap-compiler"
        val bootstrapCompilerHome = bootstrapHome / "Kotlin/kotlinc"
        val bootstrapRuntime = file % bootstrapHome / "Kotlin/lib/kotlin-runtime".jar
        val bootstrapReflect = file % bootstrapHome / "Kotlin/lib/kotlin-reflect".jar
        val bootstrapCompilerJar = file % bootstrapCompilerHome / "lib/kotlin-compiler".jar
        val bootstrapCompilerScript = bootstrapCompilerHome / "bin/kotlinc"
        val ideaSdkDir = rootDir / "ideaSDK"

        val uncheckedCompilerJar = file % "out/kb/artifacts/kotlin-compiler-unchecked.jar".jar
        val checkedCompilerJar = file % "out/kb/artifacts/kotlin-compiler-checked.jar".jar
        val outputCompilerDir = outputDir / "kotlinc"
        val outputCompilerLibsDir = outputCompilerDir / "lib"
        val outputCompilerLibsFolder = folder.jar % outputCompilerLibsDir
        
        val outputPreloaderJar = file % outputCompilerLibsFolder / "kotlin-preloader".jar
        val outputCompilerJar = file % outputCompilerLibsFolder / "kotlin-compiler".jar
        val outputBootstrapRuntime = file % outputCompilerLibsFolder / "kotlin-runtime-internal-bootstrap".jar
        val outputBootstrapReflect = file % outputCompilerLibsFolder / "kotlin-reflect-internal-bootstrap".jar

        module("kotlin") {

            version("ATTEMPT-0.1")
            val buildnoString = "snapshot.07"

            env.libDir = outputDir / "libs"
            env.tempDir = outputDir / "tmp"
            env.defaultTargetDir = outputCompilerLibsDir

            depends on children

            val properties = java.util.Properties()

            val buildno = variable(artifacts.config, buildnoString)

            val build_txt = file % outputCompilerDir / "build.txt".res

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
                    "js/js.serializer").map { folder.src % it }


            val readProperties = module("readProperties") {

                build using tools.custom with {
                    invoke { sources, targets ->
                        val target = targets.first() as VariableArtifact<java.util.Properties>
                        target.ref.clear()
                        for (src in sources)
                            openFileSet(src).coll.forEach { target.ref.load(openInputStream(it).inputStream) }
                        listOf(VariableData(target))
                    }
                    from (file % "resources/kotlinManifest.properties".cfg)
                    into (variable(artifacts.config, properties))
                }
            }


            val prepareDist = module("prepareDist") {
                copy with {
                    from (folder.bin % "compiler/cli/bin")
                    into (folder.bin % outputCompilerDir / "bin")
                }
                copy from bootstrapRuntime into outputBootstrapRuntime
                copy from bootstrapReflect into outputBootstrapReflect
                build using(tools.echo) from version!! into build_txt
            }


            fun brandedJarTool() = tools.jar with {
                dependsOn (readProperties)
                dependsOn (buildno)
                from (build_txt, prefix = "META-INF")
                addManifestProperty("Built-By", { "${properties.get("manifest.impl.vendor")}" })
                addManifestProperty("Implementation-Vendor", { "${properties.get("manifest.impl.vendor")}" })
                addManifestProperty("Implementation-Version", { "${buildno.ref}" })
            }


            val protobufLite = module("protobufLite") {
                // choose the right one
                //val originalProtobuf = library("com.google.protobuf:protobuf-java:2.5.0")
                val originalProtobuf = file % ideaSdkDir / "lib/protobuf-2.5.0.jar".jar
                val protobufLite = file % env.libDir / "protobuf-2.5.0-lite.jar".jar

                build using tools.custom { srcs, tgts ->
                    val target = tgts.first() as FileArtifact
                    target.path.getParent().toFile().mkdirs()

                    val res = run(bootstrapCompilerScript.toString(), "-script", escape4cli(rootDir / "generators/infrastructure/build-protobuf-lite.kts"),
                            escape4cli(srcs.getPaths().first()),
                            escape4cli(target.path))
                    if (res > 0)
                        throw Exception("Serializing builtins failed with error code $res")
                    openFileSet(target).coll
                } from originalProtobuf into protobufLite
            }


            val serializeBuiltins = module("serialize-builtins") {

                build using tools.custom with {
                    invoke { srcs, tgts ->
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
                        openFileSet(target).coll
                    }
                    from (folder(artifacts.source, "core/builtins/native"),
                            folder(artifacts.source, "core/builtins/src"))
                    into (folder(artifacts.binary, outputDir / "builtins"))
                }
            }


            val filterSerializedBuiltins = module("filter-serialized-builtins") {
                build using transformTargets (serializeBuiltins, { files(artifacts.binary, it as PathBasedArtifact, "kotlin/**").exclude("kotlin/internal/**", "kotlin/reflect/**") })
            }


            val compilerClasses = module("compiler-classes") {

                // could be partially shared with jar contents
                val libs = artifactsSet(
                        file % ideaSdkDir / "lib/protobuf-2.5.0".jar,
                        file % ideaSdkDir / "jps/jps-model".jar,
                        file % "dependencies/jline".jar,
                        file % "dependencies/cli-parser-1.1.1".jar,
                        files.jar % ideaSdkDir / "core" + "*.jar",
                        files.jar % "lib" + "*.jar" + "**/*.jar"
                )
                build using bootstrapCompiler() with {
                    from (compilerSourceRoots)
                    classpath (bootstrapRuntime, libs)
                    export = true
                }

           }

            val compiler = module("compiler", "Kotlin Compiler") {

                val jarContent = artifactsSet(
                        files.jar % "lib" + "*.jar",
                        files.jar % ideaSdkDir / "core" + "*.jar" - "util.jar",
                        files.jar % ideaSdkDir +
                                "jps/jps-model.jar" +
                                "lib/jna-utils.jar" +
                                "lib/oromatcher.jar" +
                                "lib/protobuf-2.5.0.jar",
                        files.jar % "." + "dependencies/jline.jar" +
                                          "dependencies/cli-parser-1.1.1.jar",
                        files.res % "compiler/frontend.java/src" + "META-INF/services/**",
                        files.res % "compiler/backend/src" + "META-INF/services/**",
                        files.res % "resources" + "kotlinManifest.properties",
                        files.res % "idea/src" + "META-INF/extensions/common.xml" +
                                                      "META-INF/extensions/kotlin2jvm.xml" +
                                                      "META-INF/extensions/kotlin2js.xml"
                )

                val makeUncheckedJar = build(jar, test, check) using brandedJarTool() with {
                    dependsOn (prepareDist)
                    // \todo implement derived artifact dependency support (files from serializedBuiltins in this case
                    from (compilerClasses, jarContent, filterSerializedBuiltins)
                    into (uncheckedCompilerJar)
                    deflate = true
                    addManifestProperty("Implementation-Title", { "${properties.get("manifest.impl.title.kotlin.compiler")}" })
                    addManifestProperty("Main-Class", "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
                    addManifestProperty("Class-Path", listOf(outputBootstrapRuntime, outputBootstrapReflect).map { it.path.getFileName() }.joinToString(" "))
                }

//                build(jar, test) using tools.copy from makeUncheckedJar export outputCompilerJar

                // now the unchecked jar doesn't run without proguarding it (the same problem exists in the original build system)
                // \todo find out why how to fix it

                val makeCheckedJar = build(check) using tools.proguard from makeUncheckedJar into checkedCompilerJar with {
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
                           -dontwarn org.objectweb.asm.**""" //  # this is ASM3, the old version that we do not use
                    )

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

                build(check) using tools.copy from makeCheckedJar into outputCompilerJar

                default(jar) // default build scenario, '*'/null if not specified (means - all)
            }


            fun newJVMCompiler() = KotlinJavaToolRule("New JVM compiler", kotlin = tools.kotlin(outputCompilerJar.path)) with {
                    kotlin.dependsOn (compiler)
                }


            fun newJSCompiler() = tools.kotlinjs(outputCompilerJar.path) with {
                    dependsOn (compiler)
                }


            module("kotlin-compiler-sources", "Kotlin Compiler sources") {
                build using tools.jar from (compilerSourceRoots)
            }


            val preloader = module("kotlin-preloader", "Preloader") {

                val sources = folder.src % "compiler/preloader/src"

                var classes = build using(tools.javac) from sources

                build using tools.jar from classes
            }


            module("android-compiler-plugin") {

                val classes = build using tools.kotlin(bootstrapCompilerJar.path) with {
                    from (files.src % "plugins/android-compiler-plugin" + "src/**/*.kt")
                    classpath (file % ideaSdkDir / "core/intellij-core".jar,
                               compiler,
                               bootstrapRuntime)
                }

                build using(tools.jar) with {
                    dependsOn (buildno)
                    from (build_txt, prefix = "META-INF")
                    from (classes,
                          files.src % "plugins/android-compiler-plugin/src" + "META-INF/services/**")
                }
            }


            module("kotlin-jdk-annotations") {
                copy from file % "dependencies/annotations/kotlin-jdk-annotations".jar
            }


            module("kotlin-android-sdk-annotations") {
                copy from file % "dependencies/annotations/kotlin-android-sdk-annotations".jar
            }
            
            
            module("kotlin-ant", "Kotlin ant tools") {

                val antlib = library("org.apache.ant:ant:1.7.1")

                val classes = build using bootstrapCompiler() with {
                    from (folder.src % "ant")
                    classpath (bootstrapRuntime,
                               antlib,
                               preloader)
                }

                build using(brandedJarTool()) with {
                    from (classes,
                          files.src % "ant/src" + "**/*.xml")

                    addManifestProperty("Implementation-Title", { "${properties.get("manifest.impl.title.kotlin.compiler.ant.task")}" })
                    addManifestProperty("Class-Path", listOf(outputPreloaderJar, outputBootstrapRuntime, outputBootstrapReflect).map { it.path.getFileName() }.joinToString(" "))
                }
            }


            module("kotlin-compiler-for-maven", "Kotlin maven tools") {
                build using brandedJarTool() with {
                    from (compiler,
                          bootstrapRuntime) // \todo need exclude here, see original build.xml
                    addManifestProperty("Main-Class", "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
                    addManifestProperty("Implementation-Title", { "${properties.get("manifest.impl.title.kotlin.compiler")}" })
                }
            }


            val builtins = module("builtins") {

                val sources = listOf(
                    "core/builtins",
                    "core/runtime.jvm").map { folder.src % it }

                build using newJVMCompiler() with {
                    from (sources)
                    export = true
                    kotlin.enableInline = true
                    kotlin.includeRuntime = false
                }
            }


            val stdlib = module("stdlib") {

                build using tools.kotlin(outputCompilerJar.path) with {
                    from (files.src % "libraries/stdlib" + "src/**.kt")
                    classpath (builtins)
                    enableInline = true
                    includeRuntime = false
                }
            }


            val core = module("core") {
                build using newJVMCompiler() with {
                    export = true
                    from (listOf(
                        "core/descriptor.loader.java",
                        "core/descriptors",
                        "core/descriptors.runtime",
                        "core/deserialization",
                        "core/util.runtime").map { folder.src % it })
                    classpath (builtins,
                               stdlib,
                               protobufLite,
                               file % "lib/javax.inject".jar)
                }
            }


            val reflection = module("reflection") {
                build using newJVMCompiler() with {
                    export = true
                    from (folder.src % "core/reflection.jvm")
                    classpath (builtins,
                               stdlib,
                               core,
                               protobufLite)
                }
            }


            val newRuntime = module("kotlin-runtime") {
                build using brandedJarTool() with {
                    from (builtins, stdlib, filterSerializedBuiltins)
                    addManifestProperty("Implementation-Title", { "${properties.get("manifest.impl.title.kotlin.jvm.runtime")}" })
                }
            }


            val newReflect = module("kotlin-reflect") {
                build using brandedJarTool() with {
                    dependsOn(newRuntime)
                    from (reflection,
                          core,
                          protobufLite,
                          file % "lib/javax.inject".jar)
                    addManifestProperty("Implementation-Title", { "${properties.get("manifest.impl.title.kotlin.jvm.reflect")}" })
                    //addManifestProperty("Class-Path", outputRuntime.path.getFileName().toString())
                    addManifestProperty("Class-Path", (newRuntime.targets().first() as FileArtifact).path.getFileName().toString())
                }
            }


            val newRuntimeSources = module("kotlin-runtime-sources") {
                build using brandedJarTool() with {
                    from (listOf(
                          "core/builtins/native",
                          "core/builtins/src",
                          "core/descriptor.loader.java/src",
                          "core/descriptors/src",
                          "core/descriptors.runtime/src",
                          "core/deserialization/src",
                          "core/reflection.jvm/src",
                          "core/runtime.jvm/src",
                          "core/util.runtime/src",
                          "libraries/stdlib/src").map { files(artifacts.source, it, "**/*") }
                    )
                    addManifestProperty("Implementation-Title", { "${properties.get("manifest.impl.title.kotlin.jvm.runtime.sources")}" })
                }
            }


            module("kotlin-for-upsource") {

                val classes = build using bootstrapCompiler() with {
                    from (listOf("idea/ide-common", "idea/idea-analysis").map { files.src % it + "src/**" })
                    classpath (files(artifacts.jar, ideaSdkDir/ "lib", "*.jar"),
                               uncheckedCompilerJar,
                               bootstrapRuntime)
                }

                build using brandedJarTool() with {
                    // \todo implement filtering of jar content to finish the module (exclude javax stuff)
                    from (classes,
                          compilerClasses,
                          filterSerializedBuiltins,
                          files.src % "idea/idea-analysis/src"
                                  + "**"
                                  - "**/*.java"
                                  - "**/*.kt",
                          files.src % "compiler/frontend.java/src" + "META-INF/services/**",
                          files.src % "compiler/backend/src" + "META-INF/services/**",
                          files.jar % "lib" + "*.jar" + "**/*.jar",
                          newRuntime,
                          newRuntimeSources,
                          newReflect,
                          folder.res % "resources",
                          folder.res % "idea/resources",
                          files.src % "idea/src" + "META-INF/**")
                }
            }


            val kjsBuiltinsSources = artifactsSet( listOf(
                    "native/kotlin/Iterator.kt",
                    "native/kotlin/Collections.kt",
                    "src/kotlin/ExtensionFunctions.kt",
                    "src/kotlin/Functions.kt",
                    "src/kotlin/Iterators.kt",
                    "src/kotlin/Range.kt",
                    "src/kotlin/FloatingPointConstants.kt"
                ).map { file % "core/builtins" / it.src } )

            val kjsStdlibSources = artifactsSet(
                    files.src % "js/js.libraries" + "src/**/*.kt",
                    files.src % "core/builtins" + "src/kotlin/reflect/*.kt" +
                                                     "src/kotlin/reflect/**/*.kt",
                    (files.src % "libraries/stdlib/src" + "**/*.kt").exclude(
                            "**/*JVM.kt",
                            "kotlin/jvm/**",
                            "kotlin/beans/**",
                            "kotlin/browser/**",
                            "kotlin/concurrent/**",
                            "kotlin/io/**",
                            "kotlin/math/**",
                            "kotlin/template/**",
                            // Temporary disabled: TODO fix: (84, 27) Unresolved reference: copyOf (_SpecialJVM.kt)
                            "kotlin/collections/ImmutableArrayList.kt",
                            // TODO fix: AllModules is subclass of ThreadLocal.
                            "kotlin/modules/**"))


            module("kotlin-jslib") {

                val builtinsJs = file % outputDir / "builtins".src("js")
                val stdlibJs = file % outputDir / "jslib.js".src("js")
                val stdlibJsMeta = file % outputDir / "stdlib-meta.js".src("js")

                build using newJSCompiler() with {
                    from (kjsBuiltinsSources)
                    into (builtinsJs)
                    meta (file % outputDir / "builtins-meta".src("js"))
                    includeStdlib = false
                }

                build using newJSCompiler() with {
                    from (kjsStdlibSources)
                    into (stdlibJs)
                    meta (stdlibJsMeta)
                    includeStdlib = false
                }

                val compiledJsFiles = build using tools.closureCompiler with {
                    from (builtinsJs,
                          stdlibJs,
                          (files.src % "js/js.translator/testData")
                                    .include("kotlin_lib_ecma5.js", "kotlin_lib.js", "maps.js", "long.js", "export_Kotlin_if_possible.js"))
                    extern (file % "js/js.translator/testData/externs.js".src)
                    into (file % outputDir / "kotlin".src("js"))
                }

                build using brandedJarTool() with {
                    from (compiledJsFiles, stdlibJsMeta, kjsStdlibSources)
                    addManifestProperty("Implementation-Title", { "${properties.get("manifest.impl.title.kotlin.javascript.stdlib")}" })
                    addManifestProperty("Specification-Title", { "${properties.get("manifest.spec.title.kotlin.javascript.lib")}" })
                }
            }


            module("kotlin-jslib-sources") {

                build using brandedJarTool() with {
                    from (kjsBuiltinsSources)
                    from (kjsStdlibSources)
                    addManifestProperty("Implementation-Title", { "${properties.get("manifest.impl.title.kotlin.jvm.runtime.sources")}" })
                }
            }
        }
        /// BUILD SCRIPT
    }

    val indent = utils.TwoSpaceIndentLn()

    println("\n--- script ------------------------------")
    println(script.nicePrint(indent))

    val scenarios = Scenarios.All
    //val scenarios = scenarios(jar)
    val graph = script.buildGraph()

    println("\n--- roots -------------------------------")
    graph.roots(scenarios).forEach { println(it.nicePrint(indent, graph, scenarios)) }
    println("\n--- leafs -------------------------------")
    graph.leafs(scenarios).forEach { println(it.nicePrint(indent, graph, scenarios)) }
    println("\n--- plan --------------------------------")
    println(graph.nicePrint(indent, scenarios))

    println("\n--- build -------------------------------")

    val cachePath = (rootDir / "out/kb/cache")
    val cacheFile = (cachePath / "source-hashes.cache").toFile()
    val useDetailedCashe = true
    val detailedCacheFile = (cachePath / "detailed-source-hashes.cache").toFile()
    fun makeContext(): GraphBuildContext {
        if (cacheFile.exists()) {
            try {
                val strm = ObjectInputStream(BufferedInputStream(FileInputStream(cacheFile)))
                val detHashes =
                        if (useDetailedCashe)
                            if (detailedCacheFile.exists()) ObjectInputStream(BufferedInputStream(FileInputStream(detailedCacheFile))).readObject() as? MutableMap<String, ByteArray?>? ?: hashMapOf()
                            else hashMapOf()
                        else null
                return GraphBuildContext(scenarios, graph, strm.readObject() as MutableMap<String, ByteArray>, detHashes)
            }
            catch (e: Exception) {
                log.error("Failed to read source hashes: " + e.getMessage())
            }
        }
        else
            log.error("Source hashes not found, running full build")
        return GraphBuildContext(scenarios, graph)
    }

    val context = makeContext()
    graph.build(scenarios, context)

    try {
        if (!cachePath.toFile().exists())
            cachePath.toFile().mkdirs()
        else if (cacheFile.exists())
            cacheFile.delete()
        val strm = ObjectOutputStream(BufferedOutputStream(FileOutputStream(cacheFile)))
        strm.writeObject(context.sourceHashes)
        strm.flush()
        if (useDetailedCashe && context.detailedHashes != null) {
            if (detailedCacheFile.exists())
                detailedCacheFile.delete()
            val detstrm = ObjectOutputStream(BufferedOutputStream(FileOutputStream(detailedCacheFile)))
            detstrm.writeObject(context.detailedHashes)
            detstrm.flush()
        }
    }
    catch (e: Exception) {
        log.error("Failed to write source hashes: " + e.getMessage())
    }

    println("\n-- done. --------------------------------")
}

