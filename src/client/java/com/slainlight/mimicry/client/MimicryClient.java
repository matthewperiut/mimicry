package com.slainlight.mimicry.client;

import com.slainlight.mimicry.Hollowmere;
import com.slainlight.mimicry.Mimicry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.LecternScreen;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;

public class MimicryClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(MimicRenderer.LAYER, MimicModel::createLayer);
		EntityRenderers.register(Mimicry.MIMIC, MimicRenderer::new);
		ModelLayerRegistry.registerModelLayer(KnightRenderer.NPC_LAYER, () -> LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64));
		EntityRenderers.register(Hollowmere.MOSS_KNIGHT, KnightRenderer::new);
		EntityRenderers.register(Hollowmere.BLACKSMITH, BlacksmithRenderer::new);
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (!level.isClientSide() || !player.getItemInHand(hand).is(Hollowmere.ALMANAC_BOOK)) {
				return InteractionResult.PASS;
			}
			Minecraft.getInstance().gui.setScreen(new AlmanacScreen());
			return InteractionResult.SUCCESS;
		});
		// the lectern screen only learns its book on menu sync; replacing the screen inside that callback removes
		// the listener mid-iteration, so the swap is scheduled (it still runs before the lectern screen renders)
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof LecternScreen lectern) {
				lectern.getMenu().addSlotListener(new ContainerListener() {
					@Override
					public void slotChanged(AbstractContainerMenu menu, int slot, ItemStack stack) {
						if (stack.is(Hollowmere.ALMANAC_BOOK)) {
							client.schedule(() -> {
								if (client.gui.screen() == lectern) {
									client.gui.setScreen(new AlmanacScreen(lectern.getMenu()));
								}
							});
						}
					}

					@Override
					public void dataChanged(AbstractContainerMenu menu, int id, int value) {
					}
				});
			}
		});
	}
}
