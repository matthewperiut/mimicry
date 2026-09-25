package com.slainlight.mimicry.client;

import com.slainlight.mimicry.BlacksmithEntity;
import com.slainlight.mimicry.Mimicry;
//? if >=26.1 {
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
*///?}
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BlacksmithRenderer extends HumanoidMobRenderer<BlacksmithEntity, /*? if >=26.1 {*/HumanoidRenderState, HumanoidModel<HumanoidRenderState>/*?} else {*//*PlayerModel<BlacksmithEntity>*//*?}*/> {
	private static final Identifier TEXTURE = Mimicry.id("textures/entity/blacksmith.png");
	private ItemStack hammer = ItemStack.EMPTY;

	public BlacksmithRenderer(EntityRendererProvider.Context context) {
		//? if >=26.1 {
		super(context, new HumanoidModel<>(context.bakeLayer(KnightRenderer.NPC_LAYER), RenderTypes::entityTranslucent), 0.5F);
		//?} else {
		/*super(context, new PlayerModel<>(context.bakeLayer(KnightRenderer.NPC_LAYER), false), 0.5F);
		this.addLayer(new HammerLayer(context));
		*///?}
	}

	@Override
	public Identifier getTextureLocation(/*? if >=26.1 {*/HumanoidRenderState state/*?} else {*//*BlacksmithEntity smith*//*?}*/) {
		return TEXTURE;
	}

	//? if >=26.1 {
	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public void extractRenderState(BlacksmithEntity smith, HumanoidRenderState state, float partialTicks) {
		super.extractRenderState(smith, state, partialTicks);
		if (!smith.hasPose(Pose.SLEEPING)) {
			if (this.hammer.isEmpty()) {
				this.hammer = new ItemStack(Items.IRON_AXE);
			}
			this.itemModelResolver.updateForLiving(state.rightHandItemState, this.hammer, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, smith);
			state.rightHandItemStack = this.hammer;
			state.mainArm = HumanoidArm.RIGHT;
		}
	}
	//?} else {
	/*private ItemStack hammer() {
		if (this.hammer.isEmpty()) {
			this.hammer = new ItemStack(Items.IRON_AXE);
		}
		return this.hammer;
	}

	// not anonymous: some javac versions give an anonymous subclass the superclass's constructor parameter names, which clash in the 1.20.1 jar
	private final class HammerLayer extends ItemInHandLayer<BlacksmithEntity, PlayerModel<BlacksmithEntity>> {
		HammerLayer(EntityRendererProvider.Context context) {
			super(BlacksmithRenderer.this, context.getItemInHandRenderer());
		}

		@Override
		public void render(PoseStack poseStack, MultiBufferSource buffers, int light, BlacksmithEntity smith, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
			if (!smith.hasPose(Pose.SLEEPING)) {
				this.renderArmWithItem(smith, BlacksmithRenderer.this.hammer(), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT, poseStack, buffers, light);
			}
		}
	}
	*///?}
}
