import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.io.File

/**
 * Rewrites the mod's data, authored in the 26.3 format, into the format of an older Minecraft version.
 * Covers the constructs the mod uses; checked against vanilla's own files for each version.
 */
class ResourceDowngrade(private val version: String) {
    private fun before(other: String) = compareVersions(version, other) < 0

    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val compact = GsonBuilder().disableHtmlEscaping().create()

    // items added after 1.21.1, mapped to the closest one that exists there
    private val newerItems = mapOf(
        "minecraft:iron_chain" to "minecraft:chain",
        "minecraft:copper_helmet" to "minecraft:chainmail_helmet",
        "minecraft:copper_chestplate" to "minecraft:chainmail_chestplate",
        "minecraft:copper_sword" to "minecraft:stone_sword",
    )

    fun run(resources: File) {
        if (!before("26.3")) return
        resources.resolve("data").listFiles()?.forEach { namespace ->
            convert(namespace.resolve("loot_table")) { loot(it) }
            convert(namespace.resolve("advancement")) { advancement(it.asJsonObject) }
            convert(namespace.resolve("worldgen/structure")) { structure(it.asJsonObject) }
            if (before("26.1")) {
                convert(namespace.resolve("recipe")) { recipe(it.asJsonObject) }
                convert(namespace) { renameItems(it) }
            }
            if (before("1.20.5")) {
                convert(namespace.resolve("loot_table")) { legacyLoot(it) }
                convert(namespace.resolve("advancement")) { legacyAdvancement(it.asJsonObject) }
                convert(namespace.resolve("recipe")) { legacyRecipe(it.asJsonObject) }
                for ((folder, plural) in pluralFolders) {
                    namespace.resolve(folder).takeIf { it.isDirectory }?.renameTo(namespace.resolve(plural))
                }
            }
        }
    }

    private fun convert(dir: File, transform: (JsonElement) -> JsonElement) {
        if (!dir.isDirectory) return
        dir.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
            file.writeText(gson.toJson(transform(JsonParser.parseString(file.readText()))))
        }
    }

    private fun list(element: JsonElement): List<JsonElement> = if (element.isJsonArray) element.asJsonArray.toList() else listOf(element)

    private fun array(elements: List<JsonElement>) = JsonArray().apply { elements.forEach(::add) }

    // 26.3: {"type": ...}, or a string naming a predicate file; before: {"condition": ...}
    private fun condition(element: JsonElement): JsonObject {
        if (element.isJsonPrimitive) {
            return JsonObject().apply {
                addProperty("condition", "minecraft:reference")
                addProperty("name", element.asString)
            }
        }
        val source = element.asJsonObject
        val type = source["type"].asString
        val out = JsonObject()
        out.addProperty("condition", type)
        for ((key, value) in source.entrySet()) {
            when (key) {
                "type" -> {}
                "terms" -> out.add(key, array(value.asJsonArray.map(::condition)))
                "term" -> out.add(key, condition(value))
                "predicate" -> out.add(key, if (type == "minecraft:entity_properties") entityPredicate(value) else value)
                else -> out.add(key, value)
            }
        }
        return out
    }

    // a 26.3 "condition" field becomes a "conditions" list; a top-level all_of is its list of terms
    private fun conditions(element: JsonElement): JsonArray {
        if (element.isJsonObject && element.asJsonObject["type"]?.asString == "minecraft:all_of") {
            return array(element.asJsonObject["terms"].asJsonArray.map(::condition))
        }
        return array(list(element).map(::condition))
    }

    private fun function(element: JsonElement): JsonObject {
        val source = element.asJsonObject
        val type = source["type"].asString
        val out = JsonObject()
        out.addProperty("function", type)
        for ((key, value) in source.entrySet()) {
            when (key) {
                "type" -> {}
                "condition" -> out.add("conditions", conditions(value))
                "modifier" -> out.add("functions", array(list(value).map(::function)))
                "destination" -> out.add(key, if (type == "minecraft:exploration_map") JsonPrimitive(value.asString.removePrefix("#")) else value)
                "components" -> out.add(key, components(value.asJsonObject))
                else -> out.add(key, value)
            }
        }
        return out
    }

    private fun loot(element: JsonElement): JsonElement = when {
        element.isJsonArray -> array(element.asJsonArray.map(::loot))
        element.isJsonObject -> JsonObject().apply {
            for ((key, value) in element.asJsonObject.entrySet()) {
                when (key) {
                    "condition" -> add("conditions", conditions(value))
                    "modifier" -> add("functions", array(list(value).map(::function)))
                    else -> add(key, loot(value))
                }
            }
            // before 26.3 exploration_map only turns a blank map into a filled one
            val explores = getAsJsonArray("functions")?.any { it.asJsonObject["function"]?.asString == "minecraft:exploration_map" } == true
            if (explores && get("name")?.asString == "minecraft:filled_map") addProperty("name", "minecraft:map")
        }
        else -> element
    }

    // 26.1 entity predicates use plain keys ("type", "location", "flags") instead of "minecraft:" ones
    private fun entityPredicate(element: JsonElement): JsonElement {
        if (!before("26.2") || !element.isJsonObject) return element
        val out = JsonObject()
        for ((key, value) in element.asJsonObject.entrySet()) {
            val plain = if (key == "minecraft:entity_type") "type" else key.removePrefix("minecraft:")
            out.add(plain, if (plain in setOf("vehicle", "passenger", "targeted_entity")) entityPredicate(value) else value)
        }
        return out
    }

    private val entityConditionFields = setOf("player", "entity", "source_entity", "villager", "parent", "partner", "child", "projectile", "shooter")

    // before 1.21.5 enchantments sit under "levels" and text components are stored as JSON strings
    private fun components(source: JsonObject): JsonObject {
        if (!before("26.1")) return source
        val out = JsonObject()
        for ((key, value) in source.entrySet()) {
            out.add(key, when (key) {
                "minecraft:enchantments", "minecraft:stored_enchantments" -> JsonObject().apply { add("levels", value) }
                "minecraft:item_name", "minecraft:custom_name" -> JsonPrimitive(compact.toJson(value))
                "minecraft:lore" -> array(value.asJsonArray.map { JsonPrimitive(compact.toJson(it)) })
                else -> value
            })
        }
        return out
    }

    // before 1.21.2 ingredients are {"item": ...} or {"tag": ...} objects
    private fun ingredient(element: JsonElement): JsonElement = when {
        element.isJsonArray -> array(element.asJsonArray.map(::ingredient))
        element.isJsonPrimitive -> JsonObject().apply {
            val id = element.asString
            if (id.startsWith("#")) addProperty("tag", id.removePrefix("#")) else addProperty("item", id)
        }
        else -> element
    }

    private fun recipe(root: JsonObject): JsonElement {
        root.getAsJsonObject("key")?.let { key -> key.entrySet().toList().forEach { (symbol, value) -> key.add(symbol, ingredient(value)) } }
        root.getAsJsonArray("ingredients")?.let { root.add("ingredients", array(it.map(::ingredient))) }
        for (field in listOf("ingredient", "base", "addition", "template")) {
            root[field]?.let { root.add(field, ingredient(it)) }
        }
        return root
    }

    private fun renameItems(element: JsonElement): JsonElement = when {
        element.isJsonArray -> array(element.asJsonArray.map(::renameItems))
        element.isJsonObject -> JsonObject().apply { element.asJsonObject.entrySet().forEach { (key, value) -> add(key, renameItems(value)) } }
        element.isJsonPrimitive && element.asJsonPrimitive.isString -> newerItems[element.asString]?.let(::JsonPrimitive) ?: element
        else -> element
    }

    private fun advancement(root: JsonObject): JsonElement {
        root.getAsJsonObject("display")?.getAsJsonObject("icon")?.let { icon ->
            icon.getAsJsonObject("components")?.let { icon.add("components", components(it)) }
        }
        root.getAsJsonObject("criteria")?.entrySet()?.forEach { (_, criterion) ->
            val fields = criterion.asJsonObject.getAsJsonObject("conditions") ?: return@forEach
            for ((key, value) in fields.entrySet().toList()) {
                if (key in entityConditionFields && value.isJsonObject && value.asJsonObject.has("type")) {
                    fields.add(key, conditions(value))
                } else if (key == "damage" && value.isJsonObject) {
                    val damage = value.asJsonObject
                    for (entity in listOf("source_entity", "direct_entity")) {
                        damage[entity]?.let { damage.add(entity, entityPredicate(it)) }
                    }
                }
            }
        }
        return root
    }

    // 26.3 spawn overrides give "count"; before: "minCount" and "maxCount"
    private fun structure(root: JsonObject): JsonElement {
        root.getAsJsonObject("spawn_overrides")?.entrySet()?.forEach { (_, category) ->
            category.asJsonObject.getAsJsonArray("spawns")?.forEach { entry ->
                val spawn = entry.asJsonObject
                val count = spawn.remove("count") ?: return@forEach
                val (min, max) = if (count.isJsonObject) count.asJsonObject["min"] to count.asJsonObject["max"] else count to count
                spawn.add("minCount", min)
                spawn.add("maxCount", max)
            }
        }
        return root
    }

    // 1.20.1 data: plural folders, item NBT instead of components, older predicate and function shapes

    private val pluralFolders = listOf(
        "loot_table" to "loot_tables", "recipe" to "recipes", "advancement" to "advancements", "structure" to "structures",
        "tags/item" to "tags/items", "tags/block" to "tags/blocks", "tags/entity_type" to "tags/entity_types",
    )

    // banner pattern ids and dye colors as 1.20.1 NBT stores them
    private val patternCodes = mapOf(
        "minecraft:rhombus" to "mr", "minecraft:circle" to "mc", "minecraft:border" to "bo", "minecraft:curly_border" to "cbo",
        "minecraft:stripe_center" to "cs", "minecraft:cross" to "cr", "minecraft:half_horizontal" to "hh", "minecraft:gradient" to "gra",
    )
    private val colors = listOf("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple",
        "blue", "brown", "green", "red", "black")

    private fun unitalic(name: JsonElement): JsonElement {
        val text = if (name.isJsonPrimitive) JsonParser.parseString(name.asString) else name
        return if (text.isJsonObject) text.deepCopy().asJsonObject.apply { addProperty("italic", false) } else text
    }

    private fun itemPredicate(element: JsonElement): JsonElement {
        if (!element.isJsonObject) return element
        val out = JsonObject()
        for ((key, value) in element.asJsonObject.entrySet()) {
            when (key) {
                "items" -> if (value.isJsonPrimitive && value.asString.startsWith("#")) out.addProperty("tag", value.asString.removePrefix("#"))
                    else out.add("items", if (value.isJsonArray) value else array(listOf(value)))
                "predicates" -> value.asJsonObject["minecraft:enchantments"]?.let { enchantments ->
                    out.add("enchantments", array(enchantments.asJsonArray.map { e ->
                        JsonObject().apply {
                            e.asJsonObject["enchantments"]?.let { add("enchantment", it) }
                            e.asJsonObject["levels"]?.let { add("levels", it) }
                        }
                    }))
                }
                else -> out.add(key, value)
            }
        }
        return out
    }

    private fun legacyFunctions(functions: JsonArray): JsonArray = array(functions.flatMap { legacyFunction(it.asJsonObject) })

    private fun legacyFunction(source: JsonObject): List<JsonElement> {
        val f = source.deepCopy()
        f["conditions"]?.let { f.add("conditions", legacyLoot(it)) }
        when (f["function"].asString) {
            "minecraft:enchanted_count_increase" -> {
                f.addProperty("function", "minecraft:looting_enchant")
                f.remove("enchantment")
            }
            "minecraft:enchant_randomly" -> f.remove("options")
            "minecraft:exploration_map" -> f["decoration"]?.let { f.addProperty("decoration", it.asString.removePrefix("minecraft:")) }
            "minecraft:set_name" -> {
                f.remove("target")
                f.add("name", unitalic(f["name"]))
            }
            "minecraft:set_components" -> return components1201(f.getAsJsonObject("components"))
        }
        return listOf(f)
    }

    private fun components1201(components: JsonObject): List<JsonElement> {
        val out = mutableListOf<JsonElement>()
        components["minecraft:enchantments"]?.let { value ->
            val levels = if (value.isJsonObject && value.asJsonObject.has("levels")) value.asJsonObject["levels"] else value
            out += JsonObject().apply { addProperty("function", "minecraft:set_enchantments"); add("enchantments", levels) }
        }
        (components["minecraft:custom_name"] ?: components["minecraft:item_name"])?.let { name ->
            out += JsonObject().apply { addProperty("function", "minecraft:set_name"); add("name", unitalic(name)) }
        }
        val patterns = components["minecraft:banner_patterns"]?.asJsonArray
        val base = components["minecraft:base_color"]?.asString
        if (patterns != null || base != null) {
            val list = (patterns?.toList() ?: emptyList()).joinToString(",") { p ->
                val pattern = p.asJsonObject
                "{Pattern:\"${patternCodes[pattern["pattern"].asString]}\",Color:${colors.indexOf(pattern["color"].asString)}}"
            }
            val baseTag = base?.let { "Base:${colors.indexOf(it)}," } ?: ""
            out += JsonObject().apply { addProperty("function", "minecraft:set_nbt"); addProperty("tag", "{BlockEntityTag:{${baseTag}Patterns:[$list]}}") }
        }
        return out
    }

    private fun legacyLoot(element: JsonElement): JsonElement = when {
        element.isJsonArray -> array(element.asJsonArray.map(::legacyLoot))
        element.isJsonObject -> JsonObject().apply {
            val source = element.asJsonObject
            val nested = source["type"]?.takeIf { it.isJsonPrimitive }?.asString == "minecraft:loot_table"
            for ((key, value) in source.entrySet()) {
                when {
                    key == "functions" && value.isJsonArray -> add(key, legacyFunctions(value.asJsonArray))
                    key == "value" && nested -> add("name", value)
                    key == "predicate" && source["condition"]?.asString == "minecraft:match_tool" -> add(key, itemPredicate(value))
                    else -> add(key, legacyLoot(value))
                }
            }
        }
        else -> element
    }

    private fun renameStructures(element: JsonElement): JsonElement = when {
        element.isJsonArray -> array(element.asJsonArray.map(::renameStructures))
        element.isJsonObject -> JsonObject().apply {
            element.asJsonObject.entrySet().forEach { (key, value) -> add(if (key == "structures" && value.isJsonPrimitive) "structure" else key, renameStructures(value)) }
        }
        else -> element
    }

    private fun legacyAdvancement(root: JsonObject): JsonElement {
        root.getAsJsonObject("display")?.getAsJsonObject("icon")?.let { icon ->
            icon.remove("id")?.let { icon.add("item", it) }
        }
        root.getAsJsonObject("criteria")?.entrySet()?.forEach { (_, criterion) ->
            val fields = criterion.asJsonObject.getAsJsonObject("conditions") ?: return@forEach
            fields.getAsJsonArray("items")?.let { items -> fields.add("items", array(items.map(::itemPredicate))) }
        }
        return renameStructures(root)
    }

    private fun legacyRecipe(root: JsonObject): JsonElement {
        root.getAsJsonObject("result")?.let { result -> result.remove("id")?.let { result.add("item", it) } }
        return root
    }
}
