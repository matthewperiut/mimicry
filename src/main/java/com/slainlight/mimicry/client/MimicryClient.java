package com.slainlight.mimicry.client;

import com.slainlight.mimicry.Hollowmere;
import com.slainlight.mimicry.Mimicry;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.LecternScreen;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
//? if >=26.1 {
import net.minecraft.client.model.player.PlayerModel;
//?} else {
/*import net.minecraft.client.model.PlayerModel;
*///?}
import net.minecraft.client.renderer.entity.EntityRendererProvider;
//? if <26.1 {
/*import net.minecraft.resources.Identifier;
*///?}
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class MimicryClient {
	private MimicryClient() {
	}

	public interface Renderers {
		<E extends Entity> void register(EntityType<? extends E> type, EntityRendererProvider<E> provider);
	}

	public static void registerLayers(BiConsumer<ModelLayerLocation, Supplier<LayerDefinition>> layers) {
		layers.accept(MimicRenderer.LAYER, MimicModel::createLayer);
		layers.accept(KnightRenderer.NPC_LAYER, () -> LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64));
	}

	public static void registerRenderers(Renderers renderers) {
		renderers.register(Mimicry.MIMIC, MimicRenderer::new);
		renderers.register(Hollowmere.MOSS_KNIGHT, KnightRenderer::new);
		renderers.register(Hollowmere.BLACKSMITH, BlacksmithRenderer::new);
	}

	//? if <26.1 {
	/*public static void openDialog(Identifier dialog) {
		Minecraft.getInstance().setScreen(new BramDialogScreen(dialog));
	}
	*///?}

	public static boolean openAlmanac(Player player, Level level, InteractionHand hand) {
		if (!level.isClientSide() || !player.getItemInHand(hand).is(Hollowmere.ALMANAC_BOOK)) {
			return false;
		}
		Minecraft.getInstance()/*? if >=26.2 {*/.gui/*?}*/.setScreen(new AlmanacScreen());
		return true;
	}

	public static void onScreenInit(Screen screen) {
		if (!(screen instanceof LecternScreen lectern)) {
			return;
		}
		// the lectern screen only learns its book on menu sync; replacing the screen inside that callback removes
		// the listener mid-iteration, so the swap is scheduled (it still runs before the lectern screen renders)
		lectern.getMenu().addSlotListener(new ContainerListener() {
			@Override
			public void slotChanged(AbstractContainerMenu menu, int slot, ItemStack stack) {
				if (stack.is(Hollowmere.ALMANAC_BOOK)) {
					Minecraft client = Minecraft.getInstance();
					client./*? if >=26.1 {*/schedule/*?} else {*//*tell*//*?}*/(() -> {
						if (client/*? if >=26.2 {*/.gui.screen()/*?} else {*//*.screen*//*?}*/ == lectern) {
							client/*? if >=26.2 {*/.gui/*?}*/.setScreen(new AlmanacScreen(lectern.getMenu()));
						}
					});
				}
			}

			@Override
			public void dataChanged(AbstractContainerMenu menu, int id, int value) {
			}
		});
	}
}
