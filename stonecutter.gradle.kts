plugins {
    id("dev.kikugie.stonecutter")
    kotlin("jvm") apply false
    id("com.google.devtools.ksp") apply false
    id("dev.kikugie.fletching-table.fabric") apply false
}

stonecutter active "26.3-fabric"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    properties {
        tags(version, loader)
    }

    constants {
        match(loader, "fabric", "neoforge", "forge")
    }

    // plain renames between 1.21.x and 26.x; the source is written with the 26.x names
    replacements {
        string(current.parsed >= "26.1") {
            replace("ResourceLocation", "Identifier")
            replace("MobSpawnType", "EntitySpawnReason")
            replace("isDay()", "isBrightOutside()")
            replace("isNight()", "isDarkOutside()")
            replace("restrictTo(", "setHomeTo(")
            replace("hasRestriction()", "hasHome()")
            replace("getMinBuildHeight()", "getMinY()")
            replace("Blocks.CHAIN", "Blocks.IRON_CHAIN")
        }
    }
}
