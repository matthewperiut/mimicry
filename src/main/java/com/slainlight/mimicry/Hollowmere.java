package com.slainlight.mimicry;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import java.util.Set;
import java.util.function.Function;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class Hollowmere {
	public static final SoundEvent KNIGHT_STEP = Mimicry.sound("entity.moss_knight.step");
	public static final SoundEvent KNIGHT_WINDUP = Mimicry.sound("entity.moss_knight.windup");
	public static final SoundEvent KNIGHT_SWING = Mimicry.sound("entity.moss_knight.swing");
	public static final SoundEvent KNIGHT_HURT = Mimicry.sound("entity.moss_knight.hurt");
	public static final SoundEvent KNIGHT_DEATH = Mimicry.sound("entity.moss_knight.death");
	public static final SoundEvent KNIGHT_AMBIENT = Mimicry.sound("entity.moss_knight.ambient");

	public static final Block SUNSTONE_CLUSTER = block("sunstone_cluster", p -> new AmethystClusterBlock(7.0F, 10.0F, p),
		BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).forceSolidOn().noOcclusion().sound(SoundType.AMETHYST_CLUSTER).strength(1.5F)
			.lightLevel(state -> 15).pushReaction(PushReaction.POPPED));
	public static final Block SUNSTONE_LANTERN = block("sunstone_lantern", LanternBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.METAL).forceSolidOn().strength(3.5F).sound(SoundType.LANTERN).lightLevel(state -> 15)
			.noOcclusion().pushReaction(PushReaction.POPPED));

	public static final Block SUNSTONE_CHAIN = block("sunstone_chain", ChainBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN));
	public static final Block BRICK_CHIMNEY = block("brick_chimney", ChimneyBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS));
	public static final Block STONE_BRICK_CHIMNEY = block("stone_brick_chimney", ChimneyBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS));
	public static final BlockEntityType<ChimneyBlock.Flue> CHIMNEY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Mimicry.id("chimney"),
		new BlockEntityType<>(ChimneyBlock.Flue::new, java.util.Set.of(BRICK_CHIMNEY, STONE_BRICK_CHIMNEY)));

	public static final Item SUNSTONE_SHARD = Mimicry.item("sunstone_shard", Item::new, new Item.Properties());
	public static final Item KNIGHT_SIGIL = Mimicry.item("knight_sigil", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item CROWN = Mimicry.item("crown", Item::new, new Item.Properties().rarity(Rarity.EPIC).humanoidArmor(new ArmorMaterial(
		ArmorMaterials.GOLD.durability(), ArmorMaterials.GOLD.defense(), ArmorMaterials.GOLD.enchantmentValue(), ArmorMaterials.GOLD.equipSound(),
		ArmorMaterials.GOLD.toughness(), ArmorMaterials.GOLD.knockbackResistance(), ArmorMaterials.GOLD.repairIngredient(),
		ResourceKey.create(EquipmentAssets.ROOT_ID, Mimicry.id("crown"))), ArmorType.HELMET));
	// must match the number of spreads in AlmanacScreen
	public static final int ALMANAC_SPREADS = 10;
	// placeholder written-book pages, one per spread, so lecterns accept it and can count pages for comparators and page turns
	public static final Item ALMANAC_BOOK = Mimicry.item("almanac", Item::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
		.component(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(""), "Bram of the Wayside Forge", 0,
			java.util.Collections.nCopies(ALMANAC_SPREADS, Filterable.passThrough(Component.translatable("item.mimicry.almanac"))), true)));

	public static final EntityType<MossKnightEntity> MOSS_KNIGHT = entity("moss_knight",
		EntityType.Builder.of(MossKnightEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(8));
	public static final EntityType<BlacksmithEntity> BLACKSMITH = entity("blacksmith",
		EntityType.Builder.of(BlacksmithEntity::new, MobCategory.MISC).sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10));
	public static final Item MOSS_KNIGHT_SPAWN_EGG = Mimicry.item("moss_knight_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(MOSS_KNIGHT));
	public static final Item BLACKSMITH_SPAWN_EGG = Mimicry.item("blacksmith_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(BLACKSMITH));

	// 0 = not started, 1 = collecting sigils, 2 = done
	public static final AttachmentType<Integer> BRAM_QUEST = AttachmentRegistry.create(Mimicry.id("bram_quest"),
		builder -> builder.persistent(Codec.INT).copyOnDeath().initializer(() -> 0));

	public static final StructureType<SunkenKeep> SUNKEN_KEEP = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Mimicry.id("sunken_keep"), () -> SunkenKeep.CODEC);
	public static final StructureType<WaysideForge> WAYSIDE_FORGE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Mimicry.id("wayside_forge"), () -> WaysideForge.CODEC);
	public static final StructurePieceType KEEP_CAVERN = piece("keep_cavern", SunkenKeep.CavernPiece::new);
	public static final StructurePieceType KEEP_TUNNEL = piece("keep_tunnel", SunkenKeep.TunnelPiece::new);
	public static final StructurePieceType KEEP_CASTLE = templatePiece("keep_castle", CastlePiece::new);
	public static final StructurePieceType FORGE = templatePiece("wayside_forge", WaysideForge.Piece::new);
	public static final StructurePieceType FORGE_GROUNDS = piece("wayside_forge_grounds", WaysideForge.Grounds::new);

	public static final ResourceKey<LootTable> KEEP_CHEST = loot("chests/sunken_keep");
	public static final ResourceKey<LootTable> KEEP_CHEST_PRIMED = loot("primed/mimicry/chests/sunken_keep");
	public static final ResourceKey<LootTable> KEEP_ARMORY = loot("chests/sunken_keep_armory");
	public static final ResourceKey<LootTable> KEEP_HOARD = loot("chests/sunken_keep_hoard");
	public static final ResourceKey<LootTable> KINGS_COFFER = loot("chests/kings_coffer");
	public static final ResourceKey<LootTable> ALMANAC = loot("gameplay/almanac");
	public static final ResourceKey<LootTable> KEEP_MAP = loot("gameplay/keep_map");
	public static final ResourceKey<LootTable> BRAM_REWARD = loot("gameplay/bram_reward");

	private static final java.util.Map<ResourceKey<LootTable>, Float> ALMANAC_CHANCES = java.util.Map.of(
		BuiltInLootTables.SIMPLE_DUNGEON, 0.25F,
		BuiltInLootTables.ABANDONED_MINESHAFT, 0.12F,
		BuiltInLootTables.STRONGHOLD_LIBRARY, 0.35F,
		BuiltInLootTables.VILLAGE_WEAPONSMITH, 0.2F,
		BuiltInLootTables.VILLAGE_TOOLSMITH, 0.2F);

	private Hollowmere() {
	}

	static void init() {
		Workshop.init();
		FabricDefaultAttributeRegistry.register(MOSS_KNIGHT, MossKnightEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(BLACKSMITH, BlacksmithEntity.createAttributes());
		SpawnPlacements.register(MOSS_KNIGHT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);

		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			Float chance = table.unwrapKey().map(ALMANAC_CHANCES::get).orElse(null);
			if (chance != null && context.getRandom().nextFloat() < chance) {
				drops.addAll(roll(context.getLevel(), ALMANAC, context.getOptional(LootContextParams.ORIGIN), null, context.getRandom()));
			}
		});

		// run by Bram's dialog buttons; no permission check since it only acts on a nearby blacksmith
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(Commands.literal("mimicry")
			.then(Commands.literal("bram").then(Commands.argument("action", StringArgumentType.word())
				.suggests((c, b) -> {
					Set.of("accept", "trade", "turnin").forEach(b::suggest);
					return b.buildFuture();
				})
				.executes(c -> BlacksmithEntity.handleDialogAction(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "action")))))));

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> {
			output.accept(MOSS_KNIGHT_SPAWN_EGG);
			output.accept(BLACKSMITH_SPAWN_EGG);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			output.insertAfter(Items.SOUL_LANTERN, SUNSTONE_LANTERN);
			output.insertAfter(SUNSTONE_LANTERN, SUNSTONE_CHAIN);
			output.insertAfter(Items.SOUL_CAMPFIRE, BRICK_CHIMNEY, STONE_BRICK_CHIMNEY);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output -> output.insertAfter(Items.AMETHYST_CLUSTER, SUNSTONE_CLUSTER));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.insertAfter(Items.AMETHYST_SHARD, SUNSTONE_SHARD);
			output.accept(KNIGHT_SIGIL);
		});
	}

	public static java.util.List<ItemStack> roll(ServerLevel level, ResourceKey<LootTable> table, @Nullable Vec3 origin, @Nullable Entity looter) {
		return roll(level, table, origin, looter, level.getRandom());
	}

	// worldgen runs off the main thread, so it must pass its own random rather than use the level's
	public static java.util.List<ItemStack> roll(ServerLevel level, ResourceKey<LootTable> table, @Nullable Vec3 origin, @Nullable Entity looter,
		net.minecraft.util.RandomSource random) {
		LootParams.Builder params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin != null ? origin : Vec3.ZERO);
		if (looter != null) {
			params.withParameter(LootContextParams.THIS_ENTITY, looter);
		}
		return level.getServer().reloadableRegistries().getLootTable(table).getRandomItems(params.create(LootContextParamSets.CHEST), random);
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Mimicry.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
		BlockItem item = (BlockItem) Mimicry.item(name, p -> new BlockItem(block, p), new Item.Properties().useBlockDescriptionPrefix());
		item.registerBlocks(Item.BY_BLOCK, item);
		return block;
	}

	private static <T extends Entity> EntityType<T> entity(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Mimicry.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	private static StructurePieceType piece(String name, StructurePieceType.ContextlessType type) {
		return Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Mimicry.id(name), type);
	}

	private static StructurePieceType templatePiece(String name, StructurePieceType.StructureTemplateType type) {
		return Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Mimicry.id(name), type);
	}

	private static ResourceKey<LootTable> loot(String path) {
		return ResourceKey.create(Registries.LOOT_TABLE, Mimicry.id(path));
	}

	public static boolean isNear(Entity entity, BlockPos pos, double distance) {
		return entity.position().closerThan(Vec3.atCenterOf(pos), distance);
	}
}
