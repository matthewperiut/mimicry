package com.slainlight.mimicry;

import java.util.function.Function;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

public class Mimicry implements ModInitializer {
	public static final String MOD_ID = "mimicry";

	public static final GameRule<Integer> MIMIC_CHANCE = GameRuleBuilder.forInteger(10).range(0, 100).category(GameRuleCategory.MOBS)
		.buildAndRegister(id("mimic_chance"));
	public static final GameRule<Integer> PRIMED_MIMIC_CHANCE = GameRuleBuilder.forInteger(90).range(0, 100).category(GameRuleCategory.MOBS)
		.buildAndRegister(id("primed_mimic_chance"));
	public static final ResourceKey<LootTable> MIMIC_CHEST_LOOT = ResourceKey.create(Registries.LOOT_TABLE, id("chests/mimic_chest"));
	public static final TagKey<Item> MIMIC_FOOD = TagKey.create(Registries.ITEM, id("mimic_food"));

	public static final SoundEvent MIMIC_CHOMP = sound("entity.mimic.chomp");
	public static final SoundEvent MIMIC_GROWL = sound("entity.mimic.growl");
	public static final SoundEvent MIMIC_HAPPY = sound("entity.mimic.happy");
	public static final SoundEvent MIMIC_HURT = sound("entity.mimic.hurt");
	public static final SoundEvent MIMIC_DEATH = sound("entity.mimic.death");
	public static final SoundEvent MIMIC_REVEAL = sound("entity.mimic.reveal");
	public static final SoundEvent MIMIC_GULP = sound("entity.mimic.gulp");
	public static final SoundEvent MIMIC_BURP = sound("entity.mimic.burp");
	public static final SoundEvent MIMIC_HOP = sound("entity.mimic.hop");
	public static final SoundEvent MIMIC_BREATHE = sound("entity.mimic.breathe");

	private static final ResourceKey<EntityType<?>> MIMIC_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("mimic"));
	public static final EntityType<MimicEntity> MIMIC = Registry.register(BuiltInRegistries.ENTITY_TYPE, MIMIC_KEY,
		EntityType.Builder.of(MimicEntity::new, MobCategory.CREATURE).sized(0.6F, 0.875F).eyeHeight(0.6F).clientTrackingRange(10)
			.updateInterval(1).build(MIMIC_KEY)); // update every tick so leaps render smoothly; zombie width so it fits through doors

	public static final Item MIMIC_TOOTH = item("mimic_tooth", Item::new, new Item.Properties());
	public static final Item TREASURE_LENS = item("treasure_lens", TreasureLensItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	public static final Item MIMIC_CHEST = item("mimic_chest", properties -> new BlockItem(Blocks.CHEST, properties),
		new Item.Properties().rarity(Rarity.UNCOMMON).component(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(MIMIC_CHEST_LOOT, 0L))
			.component(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.CONTAINER_LOOT, true)));
	public static final Item MIMIC_SPAWN_EGG = item("mimic_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(MIMIC));

	@Override
	public void onInitialize() {
		FabricDefaultAttributeRegistry.register(MIMIC, MimicEntity.createAttributes());
		Hollowmere.init();

		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (level instanceof ServerLevel serverLevel && !player.isSpectator() && springTrap(serverLevel, hit.getBlockPos(), player)) {
				return InteractionResult.SUCCESS;
			}
			if (!player.isSpectator() && BlacksmithEntity.wakeInBed(player, level, hand, hit.getBlockPos())) {
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
			!(level instanceof ServerLevel serverLevel) || !springTrap(serverLevel, pos, player));

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(MIMIC_SPAWN_EGG));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(MIMIC_TOOTH));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(TREASURE_LENS);
			output.accept(Hollowmere.ALMANAC_BOOK);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.insertAfter(Items.CHEST, MIMIC_CHEST));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> output.insertAfter(Items.GOLDEN_HELMET, Hollowmere.CROWN));
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("mimicry"), FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.mimicry"))
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
			})
			.build());
	}

	public static boolean isMimic(ServerLevel level, BlockPos pos, BlockState state) {
		if (!state.is(Blocks.CHEST) || state.getValue(ChestBlock.TYPE) != ChestType.SINGLE || level.getDifficulty() == Difficulty.PEACEFUL) {
			return false;
		}
		if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest) || chest.getLootTable() == null) {
			return false;
		}
		ResourceKey<LootTable> lootTable = chest.getLootTable();
		int chance = lootTable.equals(MIMIC_CHEST_LOOT) || lootTable.equals(Hollowmere.KINGS_COFFER) ? 100
			: lootTable.equals(Hollowmere.KEEP_HOARD) ? 50
			: PrimedChests.isPrimed(lootTable) ? level.getGameRules().get(PRIMED_MIMIC_CHANCE)
			: lootTable.equals(BuiltInLootTables.SPAWN_BONUS_CHEST) ? 0
			: level.getGameRules().get(MIMIC_CHANCE);
		// seeded from world seed + position so the answer never changes between checks
		return RandomSource.create(level.getSeed() ^ pos.asLong() * 0x9E3779B97F4A7C15L).nextInt(100) < chance;
	}

	private static boolean springTrap(ServerLevel level, BlockPos pos, Player player) {
		BlockState state = level.getBlockState(pos);
		if (!isMimic(level, pos, state)) {
			return false;
		}
		MimicEntity mimic = MIMIC.create(level, EntitySpawnReason.TRIGGERED);
		if (mimic == null) {
			return false;
		}

		ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(pos);
		boolean king = Hollowmere.KINGS_COFFER.equals(chest.getLootTable());
		chest.unpackLootTable(player);
		for (int i = 0; i < chest.getContainerSize(); i++) {
			mimic.getInventory().addItem(chest.removeItemNoUpdate(i));
		}
		float yaw = state.getValue(ChestBlock.FACING).toYRot();
		level.removeBlock(pos, false);
		mimic.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
		mimic.setYHeadRot(yaw);
		mimic.setYBodyRot(yaw);
		mimic.setPersistenceRequired();
		if (king) {
			mimic.crown();
		}
		level.addFreshEntity(mimic);
		mimic.wake(player);
		return true;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	static SoundEvent sound(String name) {
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id(name), SoundEvent.createVariableRangeEvent(id(name)));
	}

	static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}
}
