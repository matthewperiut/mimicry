@file:Suppress("AvoidDuplicateDependencies")

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("dev.kikugie.fletching-table.fabric")
    id("net.neoforged.moddev") version "2.0.147"
    id("neoforge-mutex")
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-neoforge"

repositories {
    mavenCentral()
    // Fletching Table puts Kikugie's repositories first; keep them to its own artifacts so a timeout there can't fail the rest
    matching { it.name.startsWith("KikuGie") }.configureEach {
        (this as MavenArtifactRepository).content { includeGroupAndSubgroups("dev.kikugie") }
    }
}

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

data class ModDep(val key: String, val version: String) {
    private fun meta(suffix: String): String? = findProperty("dep.$key.$suffix")?.toString()?.takeIf { it.isNotBlank() }
    val id: String get() = meta("id") ?: key
    val coords: String? get() = meta("coords")?.replace($$"$id", id)?.replace($$"$loader", "neoforge")?.replace($$"$mc", sc.current.version)
    val base: String get() = version.substringBefore('+').substringBefore("-beta")
    val range: String get() = meta("range") ?: "[$base,)"
}

fun deps(prefix: String) = project.ext.properties
    .filterKeys { it.startsWith("$prefix.") }
    .map { (k, v) -> ModDep(k.substringAfter('.'), v.toString()) }
    .sortedBy { it.key }

val requiredDeps = deps("required")
val includeDeps = deps("include")
val optionalDeps = deps("optional")
val runtimeDeps = deps("runtime")

val neoDependencies = buildString {
    val modId = property("mod.id")
    fun block(id: String, range: String, type: String) {
        appendLine("[[dependencies.$modId]]")
        appendLine("    modId = \"$id\"")
        appendLine("    type = \"$type\"")
        appendLine("    versionRange = \"$range\"")
        appendLine()
    }
    block("neoforge", "[${property("loader.neo").toString().substringBefore('.')},)", "required")
    block("minecraft", sc.properties["mod.mc_compat"], "required")
    requiredDeps.forEach { block(it.id, it.range, "required") }
    optionalDeps.forEach { block(it.id, it.range, "optional") }
}.trimEnd()

dependencies {
    if (sc.current.parsed < "26.1") compileOnly("org.jspecify:jspecify:1.0.0")
    fun ModDep.declare(vararg configurations: String) {
        val notation = (coords ?: return).replace($$"$version", version)
        configurations.forEach { conf -> conf(notation) }
    }
    requiredDeps.forEach { it.declare("implementation") }
    includeDeps.forEach { it.declare("implementation", "jarJar") }
    optionalDeps.forEach { it.declare("compileOnly") }
    runtimeDeps.forEach { it.declare("runtimeOnly") }
}

neoForge {
    version = property("loader.neo") as String

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            gameDirectory = rootProject.file("run/${sc.current.version.replace(".", "_")}_neoforge/")
            client()
        }
        register("server") {
            gameDirectory = rootProject.file("run/${sc.current.version.replace(".", "_")}_neoforge/")
            server()
        }
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

fletchingTable {
    mixins.create("main") {
        mixin("default", "${property("mod.id")}.mixins.json") {
            env("CLIENT", "${property("mod.group")}.${property("mod.id")}.mixin.client")
        }
    }
}

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }

        val mcVersion = sc.current.version
        from(overlaysFor(rootDir, mcVersion))
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        doLast { ResourceDowngrade(mcVersion).run(destinationDir) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        val neoDepends = neoDependencies

        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            register("description", "mod.description")
            register("license", "mod.license")
            register("sources_url", "mod.sources_url")
            register("authors", "mod.authors")
            inputs.property("dependencies", neoDepends)
            put("dependencies", neoDepends)
            put("icon_key", if (sc.current.parsed >= "26.1") "iconFile" else "logoFile")
        }

        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }

        exclude("fabric.mod.json", "META-INF/mods.toml", "pack.mcmeta")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        from(jar.flatMap { it.archiveFile }, named<Jar>("sourcesJar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
