package com.slainlight.mimicry.forge;

//? if forge {
/*import com.slainlight.mimicry.client.MimicryClient;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;

final class MimicryForgeClient {
	private MimicryForgeClient() {
	}

	static void init(IEventBus modBus) {
		modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> MimicryClient.registerLayers(event::registerLayerDefinition));
		modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> MimicryClient.registerRenderers(event::registerEntityRenderer));
		MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem event) -> {
			if (MimicryClient.openAlmanac(event.getEntity(), event.getLevel(), event.getHand())) {
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.SUCCESS);
			}
		});
		MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> MimicryClient.onScreenInit(event.getScreen()));
	}
}
*///?}
