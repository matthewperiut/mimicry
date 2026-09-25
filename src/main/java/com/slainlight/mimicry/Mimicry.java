package com.slainlight.mimicry;

//? if >=26.1 {
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
//?}
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
//? if fabric && <26.1 {
/*import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
//? if <1.20.5 {
/*import net.minecraft.nbt.CompoundTag;
*///?}
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
//? if >=26.1 {
import net.minecraft.world.entity.EntitySpawnReason;
//?}
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
//? if >=26.1 {
import net.minecraft.world.flag.FeatureFlagSet;
//?}
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
//? if >=1.20.5 {
import net.minecraft.world.item.component.SeededContainerLoot;
//?}
//? if >=26.1 {
import net.minecraft.world.item.component.TooltipDisplay;
//?}
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
//? if >=26.1 {
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
//?} else {
/*import net.minecraft.world.level.GameRules;
*///?}
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

public final class Mimicry {
	public static final String MOD_ID = "mimicry";

	public static final /*? if >=1.20.5 {*/ResourceKey<LootTable>/*?} else {*//*Identifier*//*?}*/ MIMIC_CHEST_LOOT =
		/*? if >=1.20.5 {*/ResourceKey.create(Registries.LOOT_TABLE, id("chests/mimic_chest"))/*?} else {*//*id("chests/mimic_chest")*//*?}*/;
	public static final TagKey<Item> MIMIC_FOOD = TagKey.create(Registries.ITEM, id("mimic_food"));
	public static final ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("mimicry"));

	// in the order the game fills them, so later ones can use what earlier ones registered
	public static final List<ResourceKey<? extends Registry<?>>> REGISTRIES = List.of(/*? if >=1.20.5 && <26.1 {*//*Registries.ARMOR_MATERIAL, *//*?}*/Registries.SOUND_EVENT,
		Registries.BLOCK, Registries.ENTITY_TYPE, Registries.ITEM, Registries.BLOCK_ENTITY_TYPE, Registries.STRUCTURE_TYPE, Registries.STRUCTURE_PIECE
		/*? if >=26.1 {*/, Registries.GAME_RULE/*?}*/);

	//? if >=26.1 {
	public static GameRule<Integer> MIMIC_CHANCE;
	public static GameRule<Integer> PRIMED_MIMIC_CHANCE;
	//?} else {
	/*public static final GameRules.Key<GameRules.IntegerValue> MIMIC_CHANCE = percentRule("mimic_chance", 10);
	public static final GameRules.Key<GameRules.IntegerValue> PRIMED_MIMIC_CHANCE = percentRule("primed_mimic_chance", 90);
	*///?}

	public static SoundEvent MIMIC_CHOMP;
	public static SoundEvent MIMIC_GROWL;
	public static SoundEvent MIMIC_HAPPY;
	public static SoundEvent MIMIC_HURT;
	public static SoundEvent MIMIC_DEATH;
	public static SoundEvent MIMIC_REVEAL;
	public static SoundEvent MIMIC_GULP;
	public static SoundEvent MIMIC_BURP;
	public static SoundEvent MIMIC_HOP;
	public static SoundEvent MIMIC_BREATHE;
	//? if <1.21 {
	/*// the mace's sounds, which 1.20.1 lacks; MaceSounds supplies the files
	public static SoundEvent MACE_SMASH_GROUND;
	public static SoundEvent MACE_SMASH_GROUND_HEAVY;
	*///?}

	public static EntityType<MimicEntity> MIMIC;

	public static Item MIMIC_TOOTH;
	public static Item TREASURE_LENS;
	public static Item MIMIC_CHEST;
	public static Item MIMIC_SPAWN_EGG;

	private Mimicry() {
	}

	public static void register(ResourceKey<? extends Registry<?>> registry) {
		if (registry.equals(Registries.SOUND_EVENT)) {
			MIMIC_CHOMP = sound("entity.mimic.chomp");
			MIMIC_GROWL = sound("entity.mimic.growl");
			MIMIC_HAPPY = sound("entity.mimic.happy");
			MIMIC_HURT = sound("entity.mimic.hurt");
			MIMIC_DEATH = sound("entity.mimic.death");
			MIMIC_REVEAL = sound("entity.mimic.reveal");
			MIMIC_GULP = sound("entity.mimic.gulp");
			MIMIC_BURP = sound("entity.mimic.burp");
			MIMIC_HOP = sound("entity.mimic.hop");
			MIMIC_BREATHE = sound("entity.mimic.breathe");
			//? if <1.21 {
			/*MACE_SMASH_GROUND = sound("item.mace.smash_ground");
			MACE_SMASH_GROUND_HEAVY = sound("item.mace.smash_ground_heavy");
			*///?}
		} else if (registry.equals(Registries.ENTITY_TYPE)) {
			ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id("mimic"));
			// update every tick so leaps render smoothly; zombie width so it fits through doors
			MIMIC = register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder.of(MimicEntity::new, MobCategory.CREATURE)
				.sized(0.6F, 0.875F)/*? if >=1.20.5 {*/.eyeHeight(0.6F)/*?}*/.clientTrackingRange(10).updateInterval(1).build(/*? if >=26.1 {*/key/*?} else {*//*key.location().toString()*//*?}*/));
		} else if (registry.equals(Registries.ITEM)) {
			MIMIC_TOOTH = item("mimic_tooth", Item::new, new Item.Properties());
			TREASURE_LENS = item("treasure_lens", TreasureLensItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
			MIMIC_CHEST = item("mimic_chest", MimicChestItem::new, new Item.Properties().rarity(Rarity.UNCOMMON)
				/*? if >=1.20.5 {*/.component(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(MIMIC_CHEST_LOOT, 0L))/*?}*/
				/*? if >=26.1 {*/.component(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.CONTAINER_LOOT, true))/*?}*/);
			MIMIC_SPAWN_EGG = spawnEgg("mimic_spawn_egg", () -> MIMIC);
		//? if >=26.1 {
		} else if (registry.equals(Registries.GAME_RULE)) {
			MIMIC_CHANCE = percentRule("mimic_chance", 10);
			PRIMED_MIMIC_CHANCE = percentRule("primed_mimic_chance", 90);
		//?}
		}
		Hollowmere.register(registry);
	}

	public static CreativeModeTab.Builder tab(CreativeModeTab.Builder builder) {
		return builder.title(Component.translatable("itemGroup.mimicry"))
			.icon(() -> new ItemStack(MIMIC_TOOTH))
			.displayItems((parameters, output) -> {
				output.accept(MIMIC_CHEST);
				output.accept(Hollowmere.STONE_BRICK_CHIMNEY);
				output.accept(Hollowmere.BRICK_CHIMNEY);
				output.accept(Hollowmere.SUNSTONE_CLUSTER);
				output.accept(Hollowmere.SUNSTONE_SHARD);
				output.accept(Hollowmere.SUNSTONE_LANTERN);
				output.accept(Hollowmere.SUNSTONE_CHAIN);
				output.accept(Hollowmere.KNIGHT_SIGIL);
				output.accept(TREASURE_LENS);
				output.accept(MIMIC_TOOTH);
				output.accept(Hollowmere.ALMANAC_BOOK);
				output.accept(Hollowmere.CROWN);
			});
	}

	public interface TabEntries {
		void add(ItemLike item);

		void addAfter(ItemLike after, ItemLike item);
	}

	public static final List<ResourceKey<CreativeModeTab>> VANILLA_TABS = List.of(CreativeModeTabs.SPAWN_EGGS, CreativeModeTabs.INGREDIENTS,
		CreativeModeTabs.TOOLS_AND_UTILITIES, CreativeModeTabs.FUNCTIONAL_BLOCKS, CreativeModeTabs.COMBAT, CreativeModeTabs.NATURAL_BLOCKS);

	public static void fillVanillaTab(ResourceKey<CreativeModeTab> tab, TabEntries entries) {
		if (tab.equals(CreativeModeTabs.SPAWN_EGGS)) {
			entries.add(MIMIC_SPAWN_EGG);
			entries.add(Hollowmere.MOSS_KNIGHT_SPAWN_EGG);
			entries.add(Hollowmere.BLACKSMITH_SPAWN_EGG);
		} else if (tab.equals(CreativeModeTabs.INGREDIENTS)) {
			entries.add(MIMIC_TOOTH);
			entries.addAfter(Items.AMETHYST_SHARD, Hollowmere.SUNSTONE_SHARD);
			entries.add(Hollowmere.KNIGHT_SIGIL);
		} else if (tab.equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
			entries.add(TREASURE_LENS);
			entries.add(Hollowmere.ALMANAC_BOOK);
		} else if (tab.equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
			entries.addAfter(Items.CHEST, MIMIC_CHEST);
			entries.addAfter(Items.SOUL_LANTERN, Hollowmere.SUNSTONE_LANTERN);
			entries.addAfter(Hollowmere.SUNSTONE_LANTERN, Hollowmere.SUNSTONE_CHAIN);
			entries.addAfter(Items.SOUL_CAMPFIRE, Hollowmere.BRICK_CHIMNEY);
			entries.addAfter(Hollowmere.BRICK_CHIMNEY, Hollowmere.STONE_BRICK_CHIMNEY);
		} else if (tab.equals(CreativeModeTabs.COMBAT)) {
			entries.addAfter(Items.GOLDEN_HELMET, Hollowmere.CROWN);
		} else if (tab.equals(CreativeModeTabs.NATURAL_BLOCKS)) {
			entries.addAfter(Items.AMETHYST_CLUSTER, Hollowmere.SUNSTONE_CLUSTER);
		}
	}

	public static void attributes(BiConsumer<EntityType<? extends LivingEntity>, AttributeSupplier.Builder> sink) {
		sink.accept(MIMIC, MimicEntity.createAttributes());
		sink.accept(Hollowmere.MOSS_KNIGHT, MossKnightEntity.createAttributes());
		sink.accept(Hollowmere.BLACKSMITH, BlacksmithEntity.createAttributes());
	}

	public static boolean onUseBlock(Player player, Level level, InteractionHand hand, BlockPos pos) {
		if (player.isSpectator()) {
			return false;
		}
		return level instanceof ServerLevel serverLevel && (player instanceof ServerPlayer serverPlayer && KeepLoot.use(serverLevel, pos, serverPlayer)
			|| springTrap(serverLevel, pos, player)) || BlacksmithEntity.wakeInBed(player, level, hand, pos);
	}

	public static boolean canBreak(LevelAccessor level, Player player, BlockPos pos) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return true;
		}
		return !(player instanceof ServerPlayer serverPlayer && KeepLoot.breakCopy(serverLevel, pos, serverPlayer)) && !springTrap(serverLevel, pos, player);
	}

	public static boolean isMimic(ServerLevel level, BlockPos pos, BlockState state) {
		if (!state.is(Blocks.CHEST) || state.getValue(ChestBlock.TYPE) != ChestType.SINGLE || level.getDifficulty() == Difficulty.PEACEFUL) {
			return false;
		}
		var lootTable = level.getBlockEntity(pos) instanceof ChestBlockEntity chest ? lootTable(chest) : null;
		if (lootTable == null) {
			return false;
		}
		int chance = lootTable.equals(MIMIC_CHEST_LOOT) || lootTable.equals(Hollowmere.KINGS_COFFER) ? 100
			: lootTable.equals(Hollowmere.KEEP_HOARD) ? 50
			: PrimedChests.isPrimed(lootTable) ? percent(level, PRIMED_MIMIC_CHANCE)
			: lootTable.equals(BuiltInLootTables.SPAWN_BONUS_CHEST) ? 0
			: percent(level, MIMIC_CHANCE);
		// seeded from world seed + position so the answer never changes between checks
		return RandomSource.create(level.getSeed() ^ pos.asLong() * 0x9E3779B97F4A7C15L).nextInt(100) < chance;
	}

	private static boolean springTrap(ServerLevel level, BlockPos pos, Player player) {
		BlockState state = level.getBlockState(pos);
		if (!isMimic(level, pos, state)) {
			return false;
		}
		ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(pos);
		boolean king = Hollowmere.KINGS_COFFER.equals(lootTable(chest));
		chest.unpackLootTable(player);
		MimicEntity mimic = burst(level, pos, state, chest);
		if (mimic == null) {
			return false;
		}
		level.removeBlock(pos, false);
		if (king) {
			breakOut(level, pos, mimic);
			mimic.crown();
		}
		level.addFreshEntity(mimic);
		mimic.wake(player);
		return true;
	}

	// a mimic at the chest's position and facing, holding the loot
	static @Nullable MimicEntity burst(ServerLevel level, BlockPos pos, BlockState state, Container loot) {
		MimicEntity mimic = MIMIC.create(level/*? if >=26.1 {*/, EntitySpawnReason.TRIGGERED/*?}*/);
		if (mimic == null) {
			return null;
		}
		for (int i = 0; i < loot.getContainerSize(); i++) {
			mimic.getInventory().addItem(loot.removeItemNoUpdate(i));
		}
		float yaw = state.getValue(ChestBlock.FACING).toYRot();
		mimic./*? if >=26.1 {*/snapTo/*?} else {*//*moveTo*//*?}*/(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
		mimic.setYHeadRot(yaw);
		mimic.setYBodyRot(yaw);
		mimic.setPersistenceRequired();
		return mimic;
	}

	public static boolean isMimicChest(Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof ChestBlockEntity chest && MIMIC_CHEST_LOOT.equals(lootTable(chest));
	}

	public static @Nullable /*? if >=1.20.5 {*/ResourceKey<LootTable>/*?} else {*//*Identifier*//*?}*/ lootTable(RandomizableContainerBlockEntity container) {
		//? if >=1.20.5 {
		return container.getLootTable();
		//?} else {
		/*// no getter for the loot table here; a container that still has one saves only the table, not its items
		CompoundTag tag = container.saveWithoutMetadata();
		return tag.contains("LootTable") ? new Identifier(tag.getString("LootTable")) : null;
		*///?}
	}

	// NeoForged's Forge rejects Registry.register for the registries it manages, so there it goes through RegisterEvent
	public static <V, T extends V> T register(Registry<V> registry, Identifier id, T value) {
		//? if forge {
		/*com.slainlight.mimicry.forge.MimicryForge.register(registry, id, value);
		return value;
		*///?} else {
		return Registry.register(registry, id, value);
		//?}
	}

	public static <V, T extends V> T register(Registry<V> registry, ResourceKey<V> key, T value) {
		//? if forge {
		/*return register(registry, key.location(), value);
		*///?} else {
		return Registry.register(registry, key, value);
		//?}
	}

	// the King's Coffer bursts out of its throne: the 3x3 around it, from its own layer up two, breaks as if mined with a pickaxe
	private static void breakOut(ServerLevel level, BlockPos chest, MimicEntity king) {
		ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		for (BlockPos pos : BlockPos.betweenClosed(chest.offset(-1, 0, -1), chest.offset(1, 2, 1))) {
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.getDestroySpeed(level, pos) < 0) {
				continue;
			}
			if (!state.requiresCorrectToolForDrops() || pickaxe.isCorrectToolForDrops(state)) {
				Block.dropResources(state, level, pos, state.hasBlockEntity() ? level.getBlockEntity(pos) : null, king, pickaxe);
			}
			level.destroyBlock(pos, false, king);
		}
	}

	public static Identifier id(String path) {
		return /*? if >=1.21 {*/Identifier.fromNamespaceAndPath(MOD_ID, path)/*?} else {*//*new Identifier(MOD_ID, path)*//*?}*/;
	}

	static SoundEvent sound(String name) {
		return register(BuiltInRegistries.SOUND_EVENT, id(name), SoundEvent.createVariableRangeEvent(id(name)));
	}

	static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
		return register(BuiltInRegistries.ITEM, key, factory.apply(properties/*? if >=26.1 {*/.setId(key)/*?}*/));
	}

	// a supplier because Forge 1.20.1 fills the item registry before the entity type one
	static Item spawnEgg(String name, java.util.function.Supplier<EntityType<? extends Mob>> type) {
		//? if >=26.1 {
		return item(name, SpawnEggItem::new, new Item.Properties().spawnEgg(type.get()));
		//?} else if forge {
		/*// white so the already colored egg textures are not tinted
		return item(name, properties -> new net.minecraftforge.common.ForgeSpawnEggItem(type, 0xFFFFFF, 0xFFFFFF, properties), new Item.Properties());
		*///?} else {
		/*return item(name, properties -> new SpawnEggItem(type.get(), 0xFFFFFF, 0xFFFFFF, properties), new Item.Properties());
		*///?}
	}

	public static int percent(ServerLevel level, /*? if >=26.1 {*/GameRule<Integer>/*?} else {*//*GameRules.Key<GameRules.IntegerValue>*//*?}*/ rule) {
		return level.getGameRules()./*? if >=26.1 {*/get/*?} else {*//*getInt*//*?}*/(rule);
	}

	//? if >=26.1 {
	private static GameRule<Integer> percentRule(String name, int defaultValue) {
		return Registry.register(BuiltInRegistries.GAME_RULE, id(name), new GameRule<>(GameRuleCategory.MOBS, GameRuleType.INT,
			IntegerArgumentType.integer(0, 100), GameRuleTypeVisitor::visitInteger, Codec.intRange(0, 100), i -> i, defaultValue, FeatureFlagSet.of()));
	}
	//?} else if fabric {
	/*// named mimicry.<name> to match the gamerule.mimicry.<name> lang keys
	private static GameRules.Key<GameRules.IntegerValue> percentRule(String name, int defaultValue) {
		return GameRuleRegistry.register(MOD_ID + "." + name, GameRules.Category.MOBS, GameRuleFactory.createIntRule(defaultValue, 0, 100));
	}
	*///?} else {
	/*private static GameRules.Key<GameRules.IntegerValue> percentRule(String name, int defaultValue) {
		return GameRules.register(MOD_ID + "." + name, GameRules.Category.MOBS, GameRules.IntegerValue.create(defaultValue));
	}
	*///?}
}
