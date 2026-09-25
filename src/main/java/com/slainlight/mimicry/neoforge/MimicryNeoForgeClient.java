package com.slainlight.mimicry.neoforge;

//? if neoforge {
/*import com.slainlight.mimicry.Mimicry;
import com.slainlight.mimicry.client.MimicryClient;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@Mod(value = Mimicry.MOD_ID, dist = Dist.CLIENT)
public final class MimicryNeoForgeClient {
	public MimicryNeoForgeClient(IEventBus modBus) {
		modBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions.class, event -> MimicryClient.registerLayers(event::registerLayerDefinition));
		modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> MimicryClient.registerRenderers(event::registerEntityRenderer));
		NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickItem.class, event -> {
			if (MimicryClient.openAlmanac(event.getEntity(), event.getLevel(), event.getHand())) {
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.SUCCESS);
			}
		});
		NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class, event -> MimicryClient.onScreenInit(event.getScreen()));
	}
}
*///?}
