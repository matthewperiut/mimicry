package com.slainlight.mimicry.fabric;

//? if fabric {
import com.slainlight.mimicry.client.MimicryClient;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.InteractionResult;
//?} else {
/*import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
*///?}

@Entrypoint("client")
public class MimicryFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		//? if >=26.1 {
		MimicryClient.registerLayers((layer, definition) -> ModelLayerRegistry.registerModelLayer(layer, definition::get));
		MimicryClient.registerRenderers(EntityRenderers::register);
		UseItemCallback.EVENT.register((player, level, hand) ->
			MimicryClient.openAlmanac(player, level, hand) ? InteractionResult.SUCCESS : InteractionResult.PASS);
		//?} else {
		/*MimicryClient.registerLayers((layer, definition) -> EntityModelLayerRegistry.registerModelLayer(layer, definition::get));
		MimicryClient.registerRenderers(EntityRendererRegistry::register);
		UseItemCallback.EVENT.register((player, level, hand) ->
			MimicryClient.openAlmanac(player, level, hand) ? InteractionResultHolder.success(player.getItemInHand(hand)) : InteractionResultHolder.pass(ItemStack.EMPTY));
		*///?}
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> MimicryClient.onScreenInit(screen));
		//? if >=1.20.5 && <26.1 {
		/*net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.slainlight.mimicry.platform.OpenDialogPayload.TYPE,
			(payload, context) -> MimicryClient.openDialog(payload.dialog()));
		*///?} else if <1.20.5 {
		/*net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.slainlight.mimicry.Mimicry.id("open_dialog"),
			(client, handler, buf, sender) -> {
				net.minecraft.resources.Identifier dialog = buf.readResourceLocation();
				client.execute(() -> MimicryClient.openDialog(dialog));
			});
		*///?}
		//? if <26.1 {
		/*net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks(net.minecraft.client.renderer.RenderType.cutout(),
			com.slainlight.mimicry.Hollowmere.SUNSTONE_CLUSTER, com.slainlight.mimicry.Hollowmere.SUNSTONE_LANTERN, com.slainlight.mimicry.Hollowmere.SUNSTONE_CHAIN);
		*///?}
	}
}
//?}
