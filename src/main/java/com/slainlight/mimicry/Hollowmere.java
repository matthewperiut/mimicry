package com.slainlight.mimicry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
//? if >=1.20.5 && <26.1 {
/*import net.minecraft.core.Holder;
*///?}
import net.minecraft.core.Registry;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
//? if <1.20.5 {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
*///?}
import net.minecraft.network.chat.Component;
//? if <1.20.5 {
/*import net.minecraft.resources.Identifier;
*///?}
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
//? if >=1.20.5 {
import net.minecraft.server.network.Filterable;
//?}
import net.minecraft.sounds.SoundEvent;
//? if <1.20.5 {
/*import net.minecraft.world.InteractionResult;
*///?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
//? if <26.1 {
/*import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
*///?}
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
//? if >=1.20.5 {
import net.minecraft.world.item.component.WrittenBookContent;
//?} else {
/*import net.minecraft.world.item.context.UseOnContext;
*///?}
//? if >=26.1 {
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
//?}
//? if <1.20.5 {
/*import net.minecraft.world.level.Level;
*///?}
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LanternBlock;
//? if <1.20.5 {
/*import net.minecraft.world.level.block.LecternBlock;
*///?}
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
//? if <1.20.5 {
/*import net.minecraft.world.level.block.state.BlockState;
*///?}
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
//? if fabric && <26.1 {
/*import net.minecraft.world.level.storage.loot.LootPool;
*///?}
import net.minecraft.world.level.storage.loot.LootTable;
//? if fabric && >=1.20.5 && <26.1 {
/*import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
*///?} else if fabric && <1.20.5 {
/*import net.minecraft.world.level.storage.loot.entries.LootTableReference;
*///?}
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
//? if fabric && <26.1 {
/*import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
*///?}
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class Hollowmere {
	public static SoundEvent KNIGHT_STEP;
	public static SoundEvent KNIGHT_WINDUP;
	public static SoundEvent KNIGHT_SWING;
	public static SoundEvent KNIGHT_HURT;
	public static SoundEvent KNIGHT_DEATH;
	public static SoundEvent KNIGHT_AMBIENT;

	public static Block SUNSTONE_CLUSTER;
	public static Block SUNSTONE_LANTERN;
	public static Block SUNSTONE_CHAIN;
	public static Block BRICK_CHIMNEY;
	public static Block STONE_BRICK_CHIMNEY;
	public static BlockEntityType<ChimneyBlock.Flue> CHIMNEY;

	public static Item SUNSTONE_SHARD;
	public static Item KNIGHT_SIGIL;
	public static Item CROWN;
	//? if >=1.20.5 && <26.1 {
	/*private static Holder<ArmorMaterial> CROWN_MATERIAL;
	*///?}
	// must match the number of spreads in AlmanacScreen
	public static final int ALMANAC_SPREADS = 10;
	public static Item ALMANAC_BOOK;

	public static EntityType<MossKnightEntity> MOSS_KNIGHT;
	public static EntityType<BlacksmithEntity> BLACKSMITH;
	public static Item MOSS_KNIGHT_SPAWN_EGG;
	public static Item BLACKSMITH_SPAWN_EGG;

	public static StructureType<SunkenKeep> SUNKEN_KEEP;
	public static StructureType<WaysideForge> WAYSIDE_FORGE;
	public static StructurePieceType KEEP_CAVERN;
	public static StructurePieceType KEEP_TUNNEL;
	public static StructurePieceType KEEP_CASTLE;
	public static StructurePieceType FORGE;
	public static StructurePieceType FORGE_GROUNDS;

	public static final /*? if >=1.20.5 {*/ResourceKey<LootTable>/*?} else {*//*Identifier*//*?}*/ KEEP_CHEST = loot("chests/sunken_keep");
	public static final /*? if >=1.20.5 {*/ResourceKey<LootTable>/*?} else {*//*Identifier*//*?}*/ KEEP_CHEST_PRIMED = loot("primed/mimicry/chests/sunken_keep");
	public static final /*? if >=1.20.5 {*/ResourceKey<LootTable>/*?} else {*//*Identifier*//*?}*/ KEEP_ARMORY = loot("chests/sunken_keep_armory");
	public static final /*? if >=1.20.5 {*/ResourceKey<LootTable>/*?} else {*//*Identifier*//*?}*/ KEEP_HOARD = loot("chests/sunken_keep_hoard");
	public static final /*? if >=1.20.5 {*/ResourceKey<LootTable>/*?} else {*//*Identifier*//*?}*/ KINGS_COFFER = loot("chests/kings_coffer");
	public static final /*? if >=1.20.5 {*/ResourceKey<LootTable>/*?} else {*//*Identifier*//*?}*/ ALMANAC = loot("gameplay/almanac");
	public static final /*? if >=1.20.5 {*/ResourceKey<LootTable>/*?} else {*//*Identifier*//*?}*/ KEEP_MAP = loot("gameplay/keep_map");
	public static final /*? if >=1.20.5 {*/ResourceKey<LootTable>/*?} else {*//*Identifier*//*?}*/ BRAM_REWARD = loot("gameplay/bram_reward");

	private static final java.util.Map</*? if >=1.20.5 {*/ResourceKey<LootTable>/*?} else {*//*Identifier*//*?}*/, Float> ALMANAC_CHANCES = java.util.Map.of(
		BuiltInLootTables.SIMPLE_DUNGEON, 0.25F,
		BuiltInLootTables.ABANDONED_MINESHAFT, 0.12F,
		BuiltInLootTables.STRONGHOLD_LIBRARY, 0.35F,
		BuiltInLootTables.VILLAGE_WEAPONSMITH, 0.2F,
		BuiltInLootTables.VILLAGE_TOOLSMITH, 0.2F);

	private Hollowmere() {
	}

	static void register(ResourceKey<? extends Registry<?>> registry) {
		if (registry.equals(Registries.SOUND_EVENT)) {
			KNIGHT_STEP = Mimicry.sound("entity.moss_knight.step");
			KNIGHT_WINDUP = Mimicry.sound("entity.moss_knight.windup");
			KNIGHT_SWING = Mimicry.sound("entity.moss_knight.swing");
			KNIGHT_HURT = Mimicry.sound("entity.moss_knight.hurt");
			KNIGHT_DEATH = Mimicry.sound("entity.moss_knight.death");
			KNIGHT_AMBIENT = Mimicry.sound("entity.moss_knight.ambient");
		} else if (registry.equals(Registries.BLOCK)) {
			SUNSTONE_CLUSTER = block("sunstone_cluster", p -> new AmethystClusterBlock(/*? if >=26.1 {*/7.0F, 10.0F/*?} else if >=1.20.5 {*//*7.0F, 3.0F*//*?} else {*//*7, 3*//*?}*/, p),
				BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).forceSolidOn().noOcclusion().sound(SoundType.AMETHYST_CLUSTER).strength(1.5F)
					.lightLevel(state -> 15).pushReaction(PushReaction./*? if >=26.3 {*/POPPED/*?} else {*//*DESTROY*//*?}*/));
			SUNSTONE_LANTERN = block("sunstone_lantern", LanternBlock::new,
				BlockBehaviour.Properties.of().mapColor(MapColor.METAL).forceSolidOn().strength(3.5F).sound(SoundType.LANTERN).lightLevel(state -> 15)
					.noOcclusion().pushReaction(PushReaction./*? if >=26.3 {*/POPPED/*?} else {*//*DESTROY*//*?}*/));
			SUNSTONE_CHAIN = block("sunstone_chain", ChainBlock::new, BlockBehaviour.Properties./*? if >=1.20.5 {*/ofFullCopy/*?} else {*//*copy*//*?}*/(Blocks.IRON_CHAIN));
			BRICK_CHIMNEY = block("brick_chimney", ChimneyBlock::new, BlockBehaviour.Properties./*? if >=1.20.5 {*/ofFullCopy/*?} else {*//*copy*//*?}*/(Blocks.BRICKS));
			STONE_BRICK_CHIMNEY = block("stone_brick_chimney", ChimneyBlock::new, BlockBehaviour.Properties./*? if >=1.20.5 {*/ofFullCopy/*?} else {*//*copy*//*?}*/(Blocks.STONE_BRICKS));
		} else if (registry.equals(Registries.ENTITY_TYPE)) {
			MOSS_KNIGHT = entity("moss_knight",
				EntityType.Builder.of(MossKnightEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F)/*? if >=1.20.5 {*/.eyeHeight(1.62F)/*?}*/.clientTrackingRange(8));
			BLACKSMITH = entity("blacksmith",
				// player height, so a carpet under a two-block doorway doesn't block him
				EntityType.Builder.of(BlacksmithEntity::new, MobCategory.MISC).sized(0.6F, 1.8F)/*? if >=1.20.5 {*/.eyeHeight(1.62F)/*?}*/.clientTrackingRange(10));
		//? if >=1.20.5 && <26.1 {
		/*} else if (registry.equals(Registries.ARMOR_MATERIAL)) {
			ArmorMaterial gold = ArmorMaterials.GOLD.value();
			CROWN_MATERIAL = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, Mimicry.id("crown"), new ArmorMaterial(gold.defense(),
				gold.enchantmentValue(), gold.equipSound(), gold.repairIngredient(), java.util.List.of(new ArmorMaterial.Layer(Mimicry.id("crown"))),
				gold.toughness(), gold.knockbackResistance()));
		*///?}
		} else if (registry.equals(Registries.ITEM)) {
			for (Block block : java.util.List.of(SUNSTONE_CLUSTER, SUNSTONE_LANTERN, SUNSTONE_CHAIN, BRICK_CHIMNEY, STONE_BRICK_CHIMNEY)) {
				blockItem(block);
			}
			SUNSTONE_SHARD = Mimicry.item("sunstone_shard", Item::new, new Item.Properties());
			KNIGHT_SIGIL = Mimicry.item("knight_sigil", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
			//? if >=26.1 {
			CROWN = Mimicry.item("crown", Item::new, new Item.Properties().rarity(Rarity.EPIC).humanoidArmor(new ArmorMaterial(
				ArmorMaterials.GOLD.durability(), ArmorMaterials.GOLD.defense(), ArmorMaterials.GOLD.enchantmentValue(), ArmorMaterials.GOLD.equipSound(),
				ArmorMaterials.GOLD.toughness(), ArmorMaterials.GOLD.knockbackResistance(), ArmorMaterials.GOLD.repairIngredient(),
				ResourceKey.create(EquipmentAssets.ROOT_ID, Mimicry.id("crown"))), ArmorType.HELMET));
			//?} else if >=1.20.5 {
			/*// same durability as a golden helmet
			CROWN = Mimicry.item("crown", properties -> new ArmorItem(CROWN_MATERIAL, ArmorItem.Type.HELMET, properties),
				new Item.Properties().rarity(Rarity.EPIC).durability(ArmorItem.Type.HELMET.getDurability(7)));
			*///?} else {
			/*CROWN = Mimicry.item("crown", properties -> new ArmorItem(new CrownMaterial(), ArmorItem.Type.HELMET, properties),
				new Item.Properties().rarity(Rarity.EPIC));
			*///?}
			// placeholder written-book pages, one per spread, so lecterns accept it and can count pages for comparators and page turns
			//? if >=1.20.5 {
			ALMANAC_BOOK = Mimicry.item("almanac", Item::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
				.component(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(""), "Bram of the Wayside Forge", 0,
					java.util.Collections.nCopies(ALMANAC_SPREADS, Filterable.passThrough(Component.translatable("item.mimicry.almanac"))), true)));
			//?} else {
			/*ALMANAC_BOOK = Mimicry.item("almanac", AlmanacItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
			*///?}
			MOSS_KNIGHT_SPAWN_EGG = Mimicry.spawnEgg("moss_knight_spawn_egg", () -> MOSS_KNIGHT);
			BLACKSMITH_SPAWN_EGG = Mimicry.spawnEgg("blacksmith_spawn_egg", () -> BLACKSMITH);
		} else if (registry.equals(Registries.BLOCK_ENTITY_TYPE)) {
			CHIMNEY = Mimicry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Mimicry.id("chimney"),
				//? if <26.1 {
				/*BlockEntityType.Builder.of(ChimneyBlock.Flue::new, BRICK_CHIMNEY, STONE_BRICK_CHIMNEY).build(null));
				*///?} else if fabric && <26.2 {
				/*net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(ChimneyBlock.Flue::new, BRICK_CHIMNEY, STONE_BRICK_CHIMNEY).build());
				*///?} else {
				new BlockEntityType<>(ChimneyBlock.Flue::new, java.util.Set.of(BRICK_CHIMNEY, STONE_BRICK_CHIMNEY)));
				//?}
		} else if (registry.equals(Registries.STRUCTURE_TYPE)) {
			SUNKEN_KEEP = Mimicry.register(BuiltInRegistries.STRUCTURE_TYPE, Mimicry.id("sunken_keep"), () -> SunkenKeep.CODEC);
			WAYSIDE_FORGE = Mimicry.register(BuiltInRegistries.STRUCTURE_TYPE, Mimicry.id("wayside_forge"), () -> WaysideForge.CODEC);
		} else if (registry.equals(Registries.STRUCTURE_PIECE)) {
			KEEP_CAVERN = piece("keep_cavern", SunkenKeep.CavernPiece::new);
			KEEP_TUNNEL = piece("keep_tunnel", SunkenKeep.TunnelPiece::new);
			KEEP_CASTLE = templatePiece("keep_castle", CastlePiece::new);
			FORGE = templatePiece("wayside_forge", WaysideForge.Piece::new);
			FORGE_GROUNDS = piece("wayside_forge_grounds", WaysideForge.Grounds::new);
		}
	}

	public static void modifyDrops(java.util.Optional</*? if >=1.20.5 {*/ResourceKey<LootTable>/*?} else {*//*Identifier*//*?}*/> table, LootContext context, java.util.List<ItemStack> drops) {
		Float chance = table.map(ALMANAC_CHANCES::get).orElse(null);
		if (chance != null && context.getRandom().nextFloat() < chance) {
			drops.addAll(roll(context.getLevel(), ALMANAC, context./*? if >=26.3 {*/getOptional/*?} else if >=26.1 {*//*getOptionalParameter*//*?} else {*//*getParamOrNull*//*?}*/(LootContextParams.ORIGIN), null, context.getRandom()));
		}
	}

	//? if fabric && <26.1 {
	/*// Fabric API for 1.21.1 and older has no drops event, so the almanac is added to the table as an extra pool instead
	public static java.util.Optional<LootPool.Builder> almanacPool(/^? if >=1.20.5 {^/ResourceKey<LootTable>/^?} else {^//^Identifier^//^?}^/ table) {
		return java.util.Optional.ofNullable(ALMANAC_CHANCES.get(table)).map(chance -> LootPool.lootPool().when(LootItemRandomChanceCondition.randomChance(chance))
			.add(/^? if >=1.20.5 {^/NestedLootTable/^?} else {^//^LootTableReference^//^?}^/.lootTableReference(ALMANAC)));
	}

	*///?}
	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, boolean dev) {
		// run by Bram's dialog buttons; no permission check since it only acts on a nearby blacksmith
		dispatcher.register(Commands.literal("mimicry")
			.then(Commands.literal("bram").then(Commands.argument("action", StringArgumentType.word())
				.suggests((c, b) -> {
					Set.of("accept", "trade", "turnin").forEach(b::suggest);
					return b.buildFuture();
				})
				.executes(c -> BlacksmithEntity.handleDialogAction(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "action"))))));
		if (dev) {
			Workshop.registerCommand(dispatcher);
		}
	}

	public static java.util.List<ItemStack> roll(ServerLevel level, /*? if >=1.20.5 {*/ResourceKey<LootTable>/*?} else {*//*Identifier*//*?}*/ table, @Nullable Vec3 origin, @Nullable Entity looter) {
		return roll(level, table, origin, looter, level.getRandom());
	}

	// worldgen runs off the main thread, so it must pass its own random rather than use the level's
	public static java.util.List<ItemStack> roll(ServerLevel level, /*? if >=1.20.5 {*/ResourceKey<LootTable>/*?} else {*//*Identifier*//*?}*/ table, @Nullable Vec3 origin, @Nullable Entity looter,
		net.minecraft.util.RandomSource random) {
		LootParams.Builder params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin != null ? origin : Vec3.ZERO);
		if (looter != null) {
			params.withParameter(LootContextParams.THIS_ENTITY, looter);
		}
		return lootTable(level, table).getRandomItems(params.create(LootContextParamSets.CHEST), /*? if >=1.20.5 {*/random/*?} else {*//*random.nextLong()*//*?}*/);
	}

	public static LootTable lootTable(ServerLevel level, /*? if >=1.20.5 {*/ResourceKey<LootTable>/*?} else {*//*Identifier*//*?}*/ table) {
		return level.getServer()./*? if >=1.20.5 {*/reloadableRegistries/*?} else {*//*getLootData*//*?}*/().getLootTable(table);
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Mimicry.id(name));
		return Mimicry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties/*? if >=26.1 {*/.setId(key)/*?}*/));
	}

	private static void blockItem(Block block) {
		BlockItem item = (BlockItem) Mimicry.item(BuiltInRegistries.BLOCK.getKey(block).getPath(), p -> new BlockItem(block, p),
			new Item.Properties()/*? if >=26.1 {*/.useBlockDescriptionPrefix()/*?}*/);
		item.registerBlocks(Item.BY_BLOCK, item);
	}

	private static <T extends Entity> EntityType<T> entity(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Mimicry.id(name));
		return Mimicry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(/*? if >=26.1 {*/key/*?} else {*//*key.location().toString()*//*?}*/));
	}

	private static StructurePieceType piece(String name, StructurePieceType.ContextlessType type) {
		return Mimicry.register(BuiltInRegistries.STRUCTURE_PIECE, Mimicry.id(name), type);
	}

	private static StructurePieceType templatePiece(String name, StructurePieceType.StructureTemplateType type) {
		return Mimicry.register(BuiltInRegistries.STRUCTURE_PIECE, Mimicry.id(name), type);
	}

	private static /*? if >=1.20.5 {*/ResourceKey<LootTable>/*?} else {*//*Identifier*//*?}*/ loot(String path) {
		return /*? if >=1.20.5 {*/ResourceKey.create(Registries.LOOT_TABLE, Mimicry.id(path))/*?} else {*//*Mimicry.id(path)*//*?}*/;
	}

	//? if <1.20.5 {
	/*// golden helmet stats; the namespaced name resolves to mimicry:textures/models/armor/crown_layer_1.png on both loaders
	private static final class CrownMaterial implements ArmorMaterial {
		@Override
		public int getDurabilityForType(ArmorItem.Type type) {
			return ArmorMaterials.GOLD.getDurabilityForType(type);
		}

		@Override
		public int getDefenseForType(ArmorItem.Type type) {
			return ArmorMaterials.GOLD.getDefenseForType(type);
		}

		@Override
		public int getEnchantmentValue() {
			return ArmorMaterials.GOLD.getEnchantmentValue();
		}

		@Override
		public SoundEvent getEquipSound() {
			return ArmorMaterials.GOLD.getEquipSound();
		}

		@Override
		public net.minecraft.world.item.crafting.Ingredient getRepairIngredient() {
			return ArmorMaterials.GOLD.getRepairIngredient();
		}

		@Override
		public String getName() {
			return "mimicry:crown";
		}

		@Override
		public float getToughness() {
			return ArmorMaterials.GOLD.getToughness();
		}

		@Override
		public float getKnockbackResistance() {
			return ArmorMaterials.GOLD.getKnockbackResistance();
		}
	}

	// only the item places itself on a lectern here, and loot creates it without NBT, so the book pages are written then
	private static final class AlmanacItem extends Item {
		AlmanacItem(Item.Properties properties) {
			super(properties);
		}

		@Override
		public InteractionResult useOn(UseOnContext context) {
			Level level = context.getLevel();
			BlockState state = level.getBlockState(context.getClickedPos());
			if (!state.is(Blocks.LECTERN)) {
				return InteractionResult.PASS;
			}
			CompoundTag tag = context.getItemInHand().getOrCreateTag();
			if (!tag.contains("pages")) {
				ListTag pages = new ListTag();
				for (int i = 0; i < ALMANAC_SPREADS; i++) {
					pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("item.mimicry.almanac"))));
				}
				tag.put("pages", pages);
				tag.putString("title", "");
				tag.putString("author", "Bram of the Wayside Forge");
				tag.putBoolean("resolved", true);
			}
			return LecternBlock.tryPlaceBook(context.getPlayer(), level, context.getClickedPos(), state, context.getItemInHand())
				? InteractionResult.sidedSuccess(level.isClientSide()) : InteractionResult.PASS;
		}
	}

	*///?}
	public static boolean isNear(Entity entity, BlockPos pos, double distance) {
		return entity.position().closerThan(Vec3.atCenterOf(pos), distance);
	}
}
