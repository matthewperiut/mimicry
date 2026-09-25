package com.slainlight.mimicry.neoforge;

//? if neoforge {
/*import com.slainlight.mimicry.Hollowmere;
import com.slainlight.mimicry.Mimicry;
import com.slainlight.mimicry.platform.Platform;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
//? if >=26.1 {
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
//?} else {
/^import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
^///?}
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Mimicry.MOD_ID)
public final class MimicryNeoForge {
	public MimicryNeoForge(IEventBus modBus) {
		modBus.addListener(RegisterEvent.class, MimicryNeoForge::register);
		modBus.addListener(EntityAttributeCreationEvent.class, event -> Mimicry.attributes((type, builder) -> event.put(type, builder.build())));
		modBus.addListener(RegisterSpawnPlacementsEvent.class, event -> event.register(Hollowmere.MOSS_KNIGHT, SpawnPlacementTypes.ON_GROUND,
			Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE));
		modBus.addListener(BuildCreativeModeTabContentsEvent.class, event -> Mimicry.fillVanillaTab(event.getTabKey(), new Mimicry.TabEntries() {
			@Override
			public void add(ItemLike item) {
				event.accept(item);
			}

			@Override
			public void addAfter(ItemLike after, ItemLike item) {
				event.insertAfter(new ItemStack(after), new ItemStack(item), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
			}
		}));

		//? if <26.1 {
		/^modBus.addListener(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent.class, event -> event.registrar("1").playToClient(
			com.slainlight.mimicry.platform.OpenDialogPayload.TYPE, com.slainlight.mimicry.platform.OpenDialogPayload.CODEC,
			(payload, context) -> com.slainlight.mimicry.client.MimicryClient.openDialog(payload.dialog())));
		^///?}

		NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, event -> {
			if (Mimicry.onUseBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getPos())) {
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.SUCCESS);
			}
		});
		NeoForge.EVENT_BUS.addListener(/^? if >=26.1 {^/BreakBlockEvent/^?} else {^//^BreakEvent^//^?}^/.class, event -> {
			if (!Mimicry.canBreak(event.getLevel(), event.getPlayer(), event.getPos())) {
				event.setCanceled(true);
			}
		});
		NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event ->
			Hollowmere.registerCommands(event.getDispatcher(), !FMLEnvironment./^? if >=26.1 {^/isProduction()/^?} else {^//^production^//^?}^/));
	}

	private static void register(RegisterEvent event) {
		ResourceKey<? extends Registry<?>> key = event.getRegistryKey();
		Mimicry.register(key);
		if (key.equals(Registries.CREATIVE_MODE_TAB)) {
			Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Mimicry.TAB, Mimicry.tab(CreativeModeTab.builder()).build());
		} else if (key.equals(NeoForgeRegistries.Keys.ATTACHMENT_TYPES)) {
			Registry.register(NeoForgeRegistries.ATTACHMENT_TYPES, Mimicry.id("bram_quest"), Platform.BRAM_QUEST);
		} else if (key.equals(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS)) {
			Registry.register(NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Mimicry.id("almanac"), AlmanacLootModifier.CODEC);
		}
	}
}
*///?}
