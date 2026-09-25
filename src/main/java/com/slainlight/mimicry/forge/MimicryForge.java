package com.slainlight.mimicry.forge;

//? if forge {
/*import com.slainlight.mimicry.Hollowmere;
import com.slainlight.mimicry.Mimicry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@Mod(Mimicry.MOD_ID)
public final class MimicryForge {
	public MimicryForge() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.addListener(MimicryForge::onRegister);
		modBus.addListener((EntityAttributeCreationEvent event) -> Mimicry.attributes((type, builder) -> event.put(type, builder.build())));
		modBus.addListener((SpawnPlacementRegisterEvent event) -> event.register(Hollowmere.MOSS_KNIGHT, SpawnPlacements.Type.ON_GROUND,
			Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE));
		modBus.addListener((BuildCreativeModeTabContentsEvent event) -> Mimicry.fillVanillaTab(event.getTabKey(), new Mimicry.TabEntries() {
			@Override
			public void add(ItemLike item) {
				event.accept(item);
			}

			@Override
			public void addAfter(ItemLike after, ItemLike item) {
				event.getEntries().putAfter(new ItemStack(after), new ItemStack(item), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
			}
		}));
		ForgeNetwork.init();

		MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock event) -> {
			if (Mimicry.onUseBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getPos())) {
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.SUCCESS);
			}
		});
		MinecraftForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event) -> {
			if (!Mimicry.canBreak(event.getLevel(), event.getPlayer(), event.getPos())) {
				event.setCanceled(true);
			}
		});
		MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> Hollowmere.registerCommands(event.getDispatcher(), !FMLEnvironment.production));

		if (FMLEnvironment.dist == Dist.CLIENT) {
			MimicryForgeClient.init(modBus);
		}
	}

	private static RegisterEvent current;

	private static void onRegister(RegisterEvent event) {
		current = event;
		ResourceKey<? extends Registry<?>> key = event.getRegistryKey();
		Mimicry.register(key);
		if (key.equals(Registries.CREATIVE_MODE_TAB)) {
			Mimicry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Mimicry.TAB, Mimicry.tab(CreativeModeTab.builder()).build());
		} else if (key.equals(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS)) {
			event.register(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Mimicry.id("almanac"), () -> AlmanacLootModifier.CODEC);
		}
	}

	public static <V, T extends V> void register(Registry<V> registry, Identifier id, T value) {
		current.register(registry.key(), id, () -> value);
	}
}
*///?}
