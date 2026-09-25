@file:Suppress("AvoidDuplicateDependencies")
import me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("dev.kikugie.fletching-table.fabric")
    id("me.modmuss50.mod-publish-plugin")
    id("net.neoforged.moddev.legacyforge") version "2.0.147"
    id("neoforge-mutex")
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-forge"

repositories {
    mavenCentral()
}

val requiredJava = JavaVersion.VERSION_17

val compatibleVersions: List<String> = sc.properties.rawOrNull("mod", "mc_releases")
    ?.asList().orEmpty().map { it.toString() }

val forgeDependencies = buildString {
    val modId = property("mod.id")
    fun block(id: String, range: String) {
        appendLine("[[dependencies.$modId]]")
        appendLine("    modId = \"$id\"")
        appendLine("    mandatory = true")
        appendLine("    versionRange = \"$range\"")
        appendLine("    ordering = \"NONE\"")
        appendLine("    side = \"BOTH\"")
        appendLine()
    }
    block("forge", "[${property("loader.legacy").toString().substringAfter('-').substringBefore('.')},)")
    block("minecraft", sc.properties["mod.mc_compat"])
}.trimEnd()

dependencies {
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.5")!!)
    implementation(jarJar("io.github.llamalad7:mixinextras-forge:0.5.5")!!)
    compileOnly("org.jspecify:jspecify:1.0.0")
}

legacyForge {
    enable {
        neoForgeVersion = property("loader.legacy") as String
    }

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            gameDirectory = rootProject.file("run/${sc.current.version.replace(".", "_")}_forge/")
            client()
        }
        register("server") {
            gameDirectory = rootProject.file("run/${sc.current.version.replace(".", "_")}_forge/")
            server()
        }
    }
}

mixin {
    add(sourceSets.main.get(), "${property("mod.id")}.refmap.json")
    config("${property("mod.id")}.mixins.json")
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
        val refmap = "${project.property("mod.id")}.refmap.json"
        val depends = forgeDependencies

        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            register("description", "mod.description")
            register("license", "mod.license")
            register("sources_url", "mod.sources_url")
            register("authors", "mod.authors")
            inputs.property("dependencies", depends)
            put("dependencies", depends)
        }

        filesMatching("META-INF/mods.toml") { expand(props) }
        filesMatching("*.mixins.json") {
            expand("java" to mixinJava)
            filter { line -> line.replace("\"required\": true,", "\"required\": true,\n  \"refmap\": \"$refmap\",") }
        }

        exclude("fabric.mod.json", "META-INF/neoforge.mods.toml", "data/neoforge/**")
    }

    jar {
        manifest.attributes("MixinConfigs" to "${project.property("mod.id")}.mixins.json")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        from(named<Jar>("reobfJar").flatMap { it.archiveFile }, named<Jar>("sourcesJar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}

publishMods {
    file = tasks.named<Jar>("reobfJar").flatMap { it.archiveFile }
    additionalFiles.from(tasks.named("sourcesJar"))
    changelog.set(rootProject.file("CHANGELOG.md").readText())
    type.set(STABLE)
    modLoaders.add("forge")
    displayName = "${property("mod.version")} for Forge ${sc.current.version}"
    dryRun = (property("publish.dry_run") as String).toBooleanStrict()

    modrinth {
        projectId.set("${property("publish.modrinth")}")
        accessToken.set(providers.environmentVariable("MR_KEY"))
        minecraftVersions.addAll(compatibleVersions)
        environment.set(ModrinthEnvironment.valueOf(property("publish.env.mr") as String))
    }

    curseforge {
        projectId.set("${property("publish.curseforge")}")
        accessToken.set(providers.environmentVariable("CF_KEY"))
        minecraftVersions.addAll(compatibleVersions)
        client = (property("publish.env.cf.client") as String).toBooleanStrict()
        server = (property("publish.env.cf.server") as String).toBooleanStrict()
    }
}
