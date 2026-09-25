package com.slainlight.mimicry.fabric;

//? if fabric {
import com.slainlight.mimicry.Hollowmere;
import com.slainlight.mimicry.Mimicry;
//? if >=1.20.5 && <26.1 {
/*import com.slainlight.mimicry.platform.OpenDialogPayload;
*///?}
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
//? if >=26.1 {
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
//?} else {
/*import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
*///?}
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
//? if >=1.20.5 {
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
//?} else {
/*import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
*///?}
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
//? if >=1.20.5 {
import net.minecraft.world.entity.SpawnPlacementTypes;
//?}
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.levelgen.Heightmap;

@Entrypoint("main")
public class MimicryFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		Mimicry.REGISTRIES.forEach(Mimicry::register);
		//? if >=1.20.5 && <26.1 {
		/*net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(OpenDialogPayload.TYPE, OpenDialogPayload.CODEC);
		*///?}
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Mimicry.TAB, Mimicry.tab(/*? if >=26.1 {*/FabricCreativeModeTab/*?} else {*//*FabricItemGroup*//*?}*/.builder()).build());
		Mimicry.attributes(FabricDefaultAttributeRegistry::register);
		SpawnPlacements.register(Hollowmere.MOSS_KNIGHT, /*? if >=1.20.5 {*/SpawnPlacementTypes.ON_GROUND/*?} else {*//*SpawnPlacements.Type.ON_GROUND*//*?}*/,
			Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);

		UseBlockCallback.EVENT.register((player, level, hand, hit) ->
			Mimicry.onUseBlock(player, level, hand, hit.getBlockPos()) ? InteractionResult.SUCCESS : InteractionResult.PASS);
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> Mimicry.canBreak(level, player, pos));
		for (ResourceKey<CreativeModeTab> tab : Mimicry.VANILLA_TABS) {
			/*? if >=26.1 {*/CreativeModeTabEvents.modifyOutputEvent/*?} else {*//*ItemGroupEvents.modifyEntriesEvent*//*?}*/(tab).register(output -> Mimicry.fillVanillaTab(tab, new Mimicry.TabEntries() {
				@Override
				public void add(ItemLike item) {
					output.accept(item);
				}

				@Override
				public void addAfter(ItemLike after, ItemLike item) {
					output./*? if >=26.1 {*/insertAfter/*?} else {*//*addAfter*//*?}*/(after, item);
				}
			}));
		}
		//? if >=26.1 {
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> Hollowmere.modifyDrops(table.unwrapKey(), context, drops));
		//?} else if >=1.20.5 {
		/*LootTableEvents.MODIFY.register((key, table, source, registries) -> Hollowmere.almanacPool(key).ifPresent(table::withPool));
		*///?} else {
		/*LootTableEvents.MODIFY.register((resources, loot, id, table, source) -> Hollowmere.almanacPool(id).ifPresent(table::withPool));
		*///?}
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
			Hollowmere.registerCommands(dispatcher, FabricLoader.getInstance().isDevelopmentEnvironment()));
	}
}
//?}
