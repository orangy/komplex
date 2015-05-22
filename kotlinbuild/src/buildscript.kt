package kotlin.buildscript

import komplex.data.OpenFileSet
import komplex.data.VariableData
import komplex.data.openFileSet
import komplex.data.openInputStream
import komplex.dsl.*
import komplex.dsl.tools
import komplex.dsl.Module
import komplex.tools.jar.jar
import komplex.tools.jar.from
import komplex.tools.maven.maven
import komplex.model.*
import komplex.tools.*
import komplex.tools.jar.addManifestProperty
import komplex.tools.javac.JavaCompilerRule
import komplex.tools.javac.javac
import komplex.tools.proguard.filters
import komplex.tools.proguard.options
import komplex.tools.proguard.proguard
import komplex.utils
import komplex.utils.Named
import komplex.utils.div
import komplex.utils.escape4cli
import komplex.utils.runProcess
import komplex.tools.from
import komplex.tools.javascript.closureCompiler
import komplex.tools.javascript.extern
import komplex.tools.kotlin.KotlinJavaToolRule
import komplex.tools.kotlin
import komplex.tools.kotlin.kotlin
import komplex.tools.kotlin.kotlinjs
import komplex.tools.kotlin.meta
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

    val test = scenario("test")
    val jar = scenario("jar")
    val check = scenario("check")
    val publish = scenario("publish")

    val script = script {
        /// BUILD SCRIPT

        env.rootDir = rootDir

        val outputDir = rootDir / "out/kb"
        val libraries = folder(artifacts.binaries, outputDir / "libs")

        fun Module.library(id: String, version: String? = null, scenario: Scenarios = Scenarios.Default_): Module {
            val libModule = komplex.tools.maven.mavenLibrary(id, version, target = libraries)
            libModule.build using tools.maven
            this.children.add(libModule)
            return libModule
        }

        fun transformTargets<T: GenericSourceType>(source: T, fn: (ArtifactDesc) -> Artifact ): komplex.dsl.LambdaRule  {
            // \todo find more generic and safe way of implementing artifact transformation tools
            fun convert(srcs: Iterable<Pair<ArtifactDesc, ArtifactData?>>, tgts: Iterable<ArtifactDesc>): Iterable<ArtifactData> = openFileSet(tgts).coll
            return tools.custom(::convert) with {
                from (source)
                fromSources.forEach { into(fn(it)) }
            }
        }

        val bootstrapHome = rootDir / "dependencies/bootstrap-compiler"
        val bootstrapCompilerHome = bootstrapHome / "Kotlin/kotlinc"
        val bootstrapRuntime = file(artifacts.jar, bootstrapHome / "Kotlin/lib/kotlin-runtime.jar")
        val bootstrapReflect = file(artifacts.jar, bootstrapHome / "Kotlin/lib/kotlin-reflect.jar")
        val bootstrapCompilerJar = file(artifacts.jar, bootstrapCompilerHome / "lib/kotlin-compiler.jar")
        val bootstrapCompilerScript = bootstrapCompilerHome / "bin/kotlinc"
        val ideaSdkDir = rootDir / "ideaSDK"

        val uncheckedCompilerJar = file(artifacts.jar, "out/kb/artifacts/kotlin-compiler-unchecked.jar")
        val checkedCompilerJar = file(artifacts.jar, "out/kb/artifacts/kotlin-compiler-checked.jar")
        val outputCompilerDir = outputDir / "kotlinc"
        val outputPreloaderJar = file(artifacts.jar, outputCompilerDir / "lib/kotlin-preloader.jar")
        val outputCompilerJar = file(artifacts.jar, outputCompilerDir / "lib/kotlin-compiler.jar")
        val outputCompilerSources = file(artifacts.jar, outputCompilerDir / "lib/kotlin-compiler-sources.jar")
        val outputBootstrapRuntime = file(artifacts.jar, outputCompilerDir / "lib/kotlin-runtime-internal-bootstrap.jar")
        val outputBootstrapReflect = file(artifacts.jar, outputCompilerDir / "lib/kotlin-reflect-internal-bootstrap.jar")
        val outputRuntime = file(artifacts.jar, outputCompilerDir / "lib/kotlin-runtime.jar")
        val outputReflect = file(artifacts.jar, outputCompilerDir / "lib/kotlin-reflect.jar")
        val outputRuntimeSources = file(artifacts.jar, outputCompilerDir / "lib/kotlin-runtime-sources.jar")
        val outputAntToolsJar = file(artifacts.jar, outputCompilerDir / "lib/kotlin-ant.jar")
        val outputMavenToolsJar = file(artifacts.jar, outputCompilerDir / "lib/kotlin-compiler-for-maven.jar")
        val outputForUpsourceJar = file(artifacts.jar, outputCompilerDir / "lib/kotlin-for-upsource.jar")
        val outputJSStdLib = file(artifacts.jar, outputCompilerDir / "lib/kotlin-jslib.jar")

        module("kotlin") {

            version("ATTEMPT-0.1")
            val buildnoString = "snapshot"

            depends on children

            val properties = java.util.Properties()

            val buildno = variable(artifacts.configs, buildnoString)

            val build_txt = file(artifacts.resources, outputCompilerDir / "build.txt")

            fun bootstrapCompiler() = KotlinJavaToolRule("Boostrap compiler", kotlin = tools.kotlin(bootstrapCompilerJar.path))

            fun bootstrapJSCompiler() = tools.kotlinjs(bootstrapCompilerJar.path)

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


            val readProperties = module("readProperties") {

                fun readProps(srcs: Iterable<Pair<ArtifactDesc, ArtifactData?>>, tgts: Iterable<ArtifactDesc>): Iterable<ArtifactData> {
                    val target = tgts.first() as VariableArtifact<java.util.Properties>
                    target.ref.clear()
                    for (src in srcs)
                        openFileSet(src).coll.forEach { target.ref.load(openInputStream(it).inputStream) }
                    return listOf(VariableData(target))
                }

                build using tools.custom(::readProps) with {
                    from (file(artifacts.configs, "resources/kotlinManifest.properties"))
                    export (variable(artifacts.configs, properties))
                }
            }


            val prepareDist = module("prepareDist") {
                build using(tools.copy) with {
                    from (folder(artifacts.binaries, "compiler/cli/bin"))
                    export (folder(artifacts.binaries, "out/kb/build.kt"))
                }
                build using(tools.copy) from bootstrapRuntime export outputBootstrapRuntime with { makeDirs = true }
                build using(tools.copy) from bootstrapReflect export outputBootstrapReflect with { makeDirs = true }
                build using(tools.echo) from version!! export build_txt
            }


            fun brandedJarTool() = tools.jar with {
                dependsOn(readProperties)
                dependsOn(buildno)
                from(build_txt, prefix = "META-INF")
                deflate = true
                addManifestProperty("Built-By", { "${properties.get("manifest.impl.vendor")}" })
                addManifestProperty("Implementation-Vendor", { "${properties.get("manifest.impl.vendor")}" })
                addManifestProperty("Implementation-Version", { "${buildno.ref}" })
            }


            val jdkAnnotations = module("jdk-annotations") {
                build using tools.copy from file(artifacts.jar, "dependencies/annotations/kotlin-jdk-annotations.jar") export folder(artifacts.jar, outputCompilerDir)
            }


            val androidSdkAnnotations = module("android-sdk-annotations") {
                build using tools.copy from file(artifacts.jar, "dependencies/annotations/kotlin-android-sdk-annotations.jar") export folder(artifacts.jar, outputCompilerDir)
            }


            val protobufLite = module("protobufLite") {
                // choose the right one
                //val originalProtobuf = library("com.google.protobuf:protobuf-java:2.5.0")
                val originalProtobuf = file(artifacts.jar, ideaSdkDir / "lib/protobuf-2.5.0.jar")
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


            val filterSerializedBuiltins = module("filter-serialized-builtins") {
                build using transformTargets (serializeBuiltins, { files(artifacts.binaries, it as PathBasedArtifact, "kotlin/**").exclude("kotlin/internal/**", "kotlin/reflect/**") })
            }

            val compilerClasses = module("compiler-classes") {

                // could be partially shared with jar contents
                val libs = artifactsSet(
                        bootstrapRuntime,
                        file(artifacts.jar, ideaSdkDir / "lib/protobuf-2.5.0.jar"),
                        file(artifacts.jar, "dependencies/jline.jar"),
                        file(artifacts.jar, "dependencies/cli-parser-1.1.1.jar"),
                        file(artifacts.jar, ideaSdkDir / "jps/jps-model.jar"),
                        files(artifacts.jar, ideaSdkDir / "core", "*.jar"),
                        files(artifacts.jar, "lib", "*.jar"),
                        files(artifacts.jar, "lib", "**/*.jar")
                )
                build using bootstrapCompiler() with {
                    from(compilerSourceRoots)
                    classpath(libs)
                    kotlin.export(folder(artifacts.binaries, "out/kb/build/compiler.kt"))
                    java.export(folder(artifacts.binaries, "out/kb/build/compiler.java"))
                }

           }

            val compiler = module("compiler", "Kotlin Compiler") {

                val jarContent = artifactsSet(
                        files(artifacts.jar, "lib", "*.jar"),
                        files(artifacts.jar, ideaSdkDir / "core", "*.jar").exclude("util.jar"),
                        file(artifacts.jar, ideaSdkDir / "jps/jps-model.jar"),
                        file(artifacts.jar, ideaSdkDir / "lib/jna-utils.jar"),
                        file(artifacts.jar, ideaSdkDir / "lib/oromatcher.jar"),
                        file(artifacts.jar, ideaSdkDir / "lib/protobuf-2.5.0.jar"),
                        file(artifacts.jar, "dependencies/jline.jar"),
                        file(artifacts.jar, "dependencies/cli-parser-1.1.1.jar"),
                        files(artifacts.resources, "compiler/frontend.java/src", "META-INF/services/**"),
                        files(artifacts.resources, "compiler/backend/src", "META-INF/services/**"),
                        files(artifacts.resources, "resources", "kotlinManifest.properties"),
                        files(artifacts.resources, "idea/src", "META-INF/extensions/common.xml"),
                        files(artifacts.resources, "idea/src", "META-INF/extensions/kotlin2jvm.xml"),
                        files(artifacts.resources, "idea/src", "META-INF/extensions/kotlin2js.xml")
                )

                val makeUncheckedJar = build(jar, test, check) using brandedJarTool() with {
                    dependsOn(prepareDist)
                    // \todo implement derived artifact dependency support (files from serializedBuiltins in this case
                    from(compilerClasses, jarContent, filterSerializedBuiltins)
                    into(uncheckedCompilerJar)
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

                build(check) using tools.copy from makeCheckedJar export outputCompilerJar

                default(jar) // default build scenario, '*'/null if not specified (means - all)
            }


            fun newJVMCompiler() = KotlinJavaToolRule("New JVM compiler", kotlin =
            tools.kotlin(outputCompilerJar.path)) with {
                    kotlin.dependsOn(compiler)
                }


            fun newJSCompiler() = tools.kotlinjs(outputCompilerJar.path) with {
                    dependsOn(compiler)
                }


            module("compiler-sources", "Kotlin Compiler sources") {
                build using tools.jar from compilerSourceRoots export outputCompilerSources with {
                    deflate = true
                }
            }


            val preloader = module("preloader", "Preloader") {

                val classes = folder(artifacts.binaries, outputDir / "build.kt/preloader")
                val sources = folder(artifacts.sources, "compiler/preloader/src")

                build using(tools.javac) from sources into classes

                build using tools.jar from classes export outputPreloaderJar with {
                    deflate = true
                }
            }


            module("ant-tools", "Kotlin ant tools") {

                val antlib = library("org.apache.ant:ant:1.7.1")

                val classes = build using bootstrapCompiler() with {
                    from(listOf(folder(artifacts.sources, rootDir / "ant" )))
                    classpath( bootstrapRuntime,
                               antlib,
                               preloader)
                    kotlin.into(folder(artifacts.binaries, "out/kb/build.kt/ant"))
                    java.into(folder(artifacts.binaries, "out/kb/build/ant"))
                }

                build using(brandedJarTool()) with {
                    from( classes,
                          files(artifacts.sources, "ant/src", "**/*.xml"))

                    addManifestProperty("Implementation-Title", { "${properties.get("manifest.impl.title.kotlin.compiler.ant.task")}" })
                    addManifestProperty("Class-Path", listOf(outputPreloaderJar, outputBootstrapRuntime, outputBootstrapReflect).map { it.path.getFileName() }.joinToString(" "))

                    export(outputAntToolsJar)
                    deflate = true
                }
            }


            module("maven-tools", "Kotlin maven tools") {
                build using brandedJarTool() with {
                    from ( compiler,
                           bootstrapRuntime) // \todo need exclude here, see original build.xml
                    export (outputMavenToolsJar)
                    addManifestProperty("Main-Class", "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
                    addManifestProperty("Implementation-Title", { "${properties.get("manifest.impl.title.kotlin.compiler")}" })
                }
            }

            val builtins = module("builtins") {

                val sources = listOf(
                    "core/builtins",
                    "core/runtime.jvm").map { folder(artifacts.sources, it) }

                build using newJVMCompiler() with {
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
                build using newJVMCompiler() with {
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
                build using newJVMCompiler() with {
                    kotlin.export (folder(artifacts.binaries, "out/kb/build.ktnew/reflection.kt"))
                    java.export (folder(artifacts.binaries, "out/kb/build.ktnew/reflection.java"))
                    from (folder(artifacts.sources, "core/reflection.jvm"))
                    classpath( builtins,
                               stdlib,
                               core,
                               protobufLite)
                }
            }


            module("pack-runtime") {
                build using brandedJarTool() with {
                    from(builtins, stdlib, filterSerializedBuiltins)
                    export(outputRuntime)
                    deflate = true
                    addManifestProperty("Implementation-Title", { "${properties.get("manifest.impl.title.kotlin.jvm.runtime")}" })
                }
                build using brandedJarTool() with {
                    from(reflection,
                         core,
                         protobufLite,
                         file(artifacts.jar, "lib/javax.inject.jar"))
                    export(outputReflect)
                    deflate = true
                    addManifestProperty("Implementation-Title", { "${properties.get("manifest.impl.title.kotlin.jvm.reflect")}" })
                    addManifestProperty("Class-Path", outputRuntime.path.getFileName().toString())
                }
            }


            module("pack-runtime-sources") {
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
                        "libraries/stdlib/src").map { files(artifacts.sources, it, "**/*") }
                    )
                    export(outputRuntimeSources)
                    deflate = true
                    addManifestProperty("Implementation-Title", { "${properties.get("manifest.impl.title.kotlin.jvm.runtime.sources")}" })
                }
            }




//            module("kotlin-for-upsource") {
//
//                val classes = build using bootstrapCompiler() with {
//                    from (listOf("idea/ide-common", "idea/idea-analysis").map { files(artifacts.sources, it, "src/**") })
//                    classpath (files(artifacts.jar, ideaSdkDir/ "lib", "*.jar"),
//                               uncheckedCompilerJar)
//                    kotlin.into(folder(artifacts.binaries, "out/kb/build/4upsource.kt"))
//                    java.into(folder(artifacts.binaries, "out/kb/build/4upsource.java"))
//                }
//
//                build using brandedJarTool() with {
//                    // \todo implement filtering of jar content to finish the module (exclude javax stuff)
//                    from ( classes,
//                           compilerClasses,
//                           filterSerializedBuiltins,
//                           files(artifacts.sources, "idea/idea-analysis/src", "**").exclude("**/*.java", "**/*.kt"),
//                           files(artifacts.sources, "compiler/frontend.java/src", "META-INF/services/**"),
//                           files(artifacts.sources, "compiler/backend/src", "META-INF/services/**"),
//                           files(artifacts.jar, "lib", "*.jar"),
//                           files(artifacts.jar, "lib", "**/*.jar"),
//                           outputRuntime,
//                           outputRuntimeSources,
//                           outputReflect,
//                           folder(artifacts.resources, "resources"),
//                           folder(artifacts.resources, "idea/resources"),
//                           files(artifacts.sources, "idea/src", "META-INF/**"))
//                    into (outputForUpsourceJar)
//                }
//            }

            module("js-stdlib") {

                val builtinsJs = file(artifacts.sources, outputDir / "builtins.js")
                val stdlibJs = file(artifacts.sources, outputDir / "jslib.js")

                build using newJSCompiler() with {
                    from (listOf(
                            "native/kotlin/Iterator.kt",
                            "native/kotlin/Collections.kt",
                            "src/kotlin/ExtensionFunctions.kt",
                            "src/kotlin/Functions.kt",
                            "src/kotlin/Iterators.kt",
                            "src/kotlin/Range.kt",
                            "src/kotlin/FloatingPointConstants.kt"
                        ).map { file(artifacts.sources, rootDir / "core/builtins" / it)})
                    into (builtinsJs)
                    meta (file(artifacts.sources, outputDir / "builtins-meta.js"))
                    includeStdlib = false
                }

                build using newJSCompiler() with {
                    from (files(artifacts.sources, "js/js.libraries", "src/**/*.kt"),
                          files(artifacts.sources, "core/builtins").include("src/kotlin/reflect/*.kt", "src/kotlin/reflect/**/*.kt"),
                          files(artifacts.sources, "libraries/stdlib/src", "**/*.kt").exclude(
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
                    into (stdlibJs)
                    meta (file(artifacts.sources, outputDir / "stdlib-meta.js"))
                    includeStdlib = false
                }


                val compiledJsFiles = build using tools.closureCompiler with {
                    from (builtinsJs,
                            stdlibJs,
                            files(artifacts.sources, "js/js.translator/testData")
                                    .include("kotlin_lib_ecma5.js", "kotlin_lib.js", "maps.js", "long.js", "export_Kotlin_if_possible.js"))
                    extern (file(artifacts.sources, "js/js.translator/testData/externs.js"))
                    into (file(artifacts.sources, outputDir / "kotlin.js"))
                }

                build using brandedJarTool() with {
                    from (compiledJsFiles)
                    export (outputJSStdLib)
                    addManifestProperty("Implementation-Title", { "${properties.get("manifest.impl.title.kotlin.javascript.stdlib")}" })
                    addManifestProperty("Specification-Title", { "${properties.get("manifest.spec.title.kotlin.javascript.lib")}" })
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
    graph.build(scenarios)

    println("\n-- done. --------------------------------")
}

