package com.slainlight.mimicry.client;

import com.slainlight.mimicry.BlacksmithEntity;
import com.slainlight.mimicry.Mimicry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BlacksmithRenderer extends HumanoidMobRenderer<BlacksmithEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	private static final Identifier TEXTURE = Mimicry.id("textures/entity/blacksmith.png");
	private ItemStack hammer = ItemStack.EMPTY;

	public BlacksmithRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(KnightRenderer.NPC_LAYER), RenderTypes::entityTranslucent), 0.5F);
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return TEXTURE;
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public void extractRenderState(BlacksmithEntity smith, HumanoidRenderState state, float partialTicks) {
		super.extractRenderState(smith, state, partialTicks);
		if (!smith.hasPose(Pose.SLEEPING)) {
			if (this.hammer.isEmpty()) {
				this.hammer = new ItemStack(Items.STONE_AXE);
			}
			this.itemModelResolver.updateForLiving(state.rightHandItemState, this.hammer, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, smith);
			state.rightHandItemStack = this.hammer;
			state.mainArm = HumanoidArm.RIGHT;
		}
	}
}
