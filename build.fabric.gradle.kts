@file:Suppress("AvoidDuplicateDependencies")

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("dev.kikugie.fletching-table.fabric")
    id("dev.kikugie.loom-back-compat")
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-fabric"

repositories {
    mavenCentral()
    // Fletching Table puts Kikugie's repositories first; keep them to its own artifacts so a timeout there can't fail the rest
    matching { it.name.startsWith("KikuGie") }.configureEach {
        (this as MavenArtifactRepository).content { includeGroupAndSubgroups("dev.kikugie") }
    }
}

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

data class ModDep(val key: String, val version: String) {
    private fun meta(suffix: String): String? = findProperty("dep.$key.$suffix")?.toString()?.takeIf { it.isNotBlank() }
    val id: String get() = meta("id") ?: key
    val coords: String? get() = meta("coords")?.replace($$"$id", id)?.replace($$"$loader", "fabric")?.replace($$"$mc", sc.current.version)
    val base: String get() = version.substringBefore('+').substringBefore("-beta")
    val range: String get() = meta("range") ?: ">=$base"
}

fun deps(prefix: String) = project.ext.properties
    .filterKeys { it.startsWith("$prefix.") }
    .map { (k, v) -> ModDep(k.substringAfter('.'), v.toString()) }
    .sortedBy { it.key }

val requiredDeps = deps("required")
val includeDeps = deps("include")
val optionalDeps = deps("optional")
val runtimeDeps = deps("runtime")

fun jsonObject(entries: List<Pair<String, String>>): String =
    if (entries.isEmpty()) "{}"
    else entries.joinToString(",\n    ", "{\n    ", "\n  }") { (k, v) -> "\"$k\": \"$v\"" }

val fabricDepends = jsonObject(
    buildList {
        add("minecraft" to sc.properties["mod.mc_compat"])
        add("fabricloader" to ">=${property("loader.fabric")}")
        add("java" to ">=${requiredJava.majorVersion}")
        requiredDeps.forEach { add(it.id to it.range) }
    }
)

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    if (sc.current.parsed < "26.1") compileOnly("org.jspecify:jspecify:1.0.0")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("loader.fabric")}")
    fun ModDep.declare(vararg configurations: String) {
        val notation = (coords ?: return).replace($$"$version", version)
        configurations.forEach { conf ->
            conf(notation) {
                if (id != "fabric-api") exclude(group = "net.fabricmc.fabric-api")
            }
        }
    }
    requiredDeps.forEach { it.declare("modImplementation") }
    includeDeps.forEach { it.declare("modImplementation", "include") }
    optionalDeps.forEach { it.declare("modCompileOnly") }
    runtimeDeps.forEach { it.declare("modRuntimeOnly") }
}

loom {
    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run/${sc.current.version.replace(".", "_")}_fabric/")
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
        val depends = fabricDepends

        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            register("description", "mod.description")
            register("license", "mod.license")
            register("sources_url", "mod.sources_url")
            register("authors", "mod.authors")
            inputs.property("depends", depends)
        }

        filesMatching("fabric.mod.json") { expand(props) }
        filesMatching("fabric.mod.json") {
            filter { line -> line.replace("\"depends\": {}", "\"depends\": $depends") }
        }
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }

        exclude("META-INF/neoforge.mods.toml", "META-INF/mods.toml", "pack.mcmeta", "data/*/loot_modifiers/**", "data/neoforge/**", "data/forge/**")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
