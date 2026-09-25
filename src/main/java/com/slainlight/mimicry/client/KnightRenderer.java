package com.slainlight.mimicry.client;

import com.slainlight.mimicry.Mimicry;
import com.slainlight.mimicry.MossKnightEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
//? if >=26.1 {
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
//?} else {
/*import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
*///?}
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class KnightRenderer extends HumanoidMobRenderer<MossKnightEntity, /*? if >=26.1 {*/KnightRenderer.State, /*?}*/KnightRenderer.Model> {
	public static final ModelLayerLocation NPC_LAYER = new ModelLayerLocation(Mimicry.id("npc"), "main");
	private static final Identifier TEXTURE = Mimicry.id("textures/entity/moss_knight.png");

	// stances: grip angle around the shoulder line (0 = down), grip x offset, blade angle (0 = up)
	private static final float[] GUARD = {0.75F, 0.0F, 0.7F};
	private static final float[] RAISED = {2.21F, -0.5F, -0.3F};
	private static final float[] STRUCK = {0.95F, 0.0F, 2.0F};
	private static final float GRIP_RADIUS = 7.5F;
	private static final float GREATSWORD_SCALE = 1.4F;

	// item components aren't bound yet when renderers are built, so the stack is made on first use
	private ItemStack greatsword = ItemStack.EMPTY;

	public KnightRenderer(EntityRendererProvider.Context context) {
		super(context, new Model(context.bakeLayer(NPC_LAYER)), 0.5F);
		this.addLayer(new GreatswordLayer(this/*? if <26.1 {*//*, context.getItemInHandRenderer()*//*?}*/));
	}

	@Override
	public Identifier getTextureLocation(/*? if >=26.1 {*/State state/*?} else {*//*MossKnightEntity knight*//*?}*/) {
		return TEXTURE;
	}

	//? if >=26.1 {
	@Override
	public State createRenderState() {
		return new State();
	}
	//?} else {
	/*@Override
	public void render(MossKnightEntity knight, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
		this.extractRenderState(knight, this.model.state, partialTicks);
		super.render(knight, yaw, partialTicks, poseStack, buffers, light);
	}
	*///?}

	/*? if >=26.1 {*/@Override/*?}*/
	public void extractRenderState(MossKnightEntity knight, State state, float partialTicks) {
		//? if >=26.1 {
		super.extractRenderState(knight, state, partialTicks);
		//?} else {
		/*boolean walking = !knight.isPassenger() && knight.isAlive();
		state.ageInTicks = knight.tickCount + partialTicks;
		state.walkAnimationPos = walking ? knight.walkAnimation.position(partialTicks) : 0.0F;
		state.walkAnimationSpeed = walking ? knight.walkAnimation.speed(partialTicks) : 0.0F;
		state.isInvisible = knight.isInvisible();
		*///?}
		if (this.greatsword.isEmpty()) {
			this.greatsword = new ItemStack(Items.STONE_SWORD);
		}
		//? if >=26.1 {
		this.itemModelResolver.updateForLiving(state.greatsword, this.greatsword, ItemDisplayContext.NONE, knight);
		//?} else {
		/*state.greatsword = this.greatsword;
		*///?}

		float t = knight.getSwingProgress(partialTicks);
		if (t < 0) {
			float breath = Mth.sin(state.ageInTicks * 0.08F);
			state.pose(GUARD, GUARD, 0.0F, 0.0F);
			state.gripAngle += breath * 0.03F;
			state.blade += breath * 0.03F;
		} else if (t < MossKnightEntity.WINDUP) {
			float p = t / MossKnightEntity.WINDUP;
			float e = 1 - (1 - p) * (1 - p);
			state.pose(GUARD, RAISED, e, -0.22F * e);
		} else if (t < MossKnightEntity.WINDUP + MossKnightEntity.STRIKE) {
			float p = (t - MossKnightEntity.WINDUP) / MossKnightEntity.STRIKE;
			float e = p * p;
			state.pose(RAISED, STRUCK, e, Mth.lerp(e, -0.22F, 0.38F));
		} else {
			float p = (t - MossKnightEntity.WINDUP - MossKnightEntity.STRIKE) / MossKnightEntity.RECOVER;
			float e = p * p * (3 - 2 * p);
			state.pose(STRUCK, GUARD, e, Mth.lerp(e, 0.38F, 0.0F));
		}
		state.striking = t >= MossKnightEntity.WINDUP && t < MossKnightEntity.WINDUP + MossKnightEntity.STRIKE + 6;
	}

	public static class State/*? if >=26.1 {*/ extends HumanoidRenderState/*?}*/ {
		//? if >=26.1 {
		public final ItemStackRenderState greatsword = new ItemStackRenderState();
		//?} else {
		/*public ItemStack greatsword = ItemStack.EMPTY;
		public float ageInTicks;
		public float walkAnimationPos;
		public float walkAnimationSpeed;
		public boolean isInvisible;
		*///?}
		public float gripAngle;
		public float gripX;
		public float blade;
		public float lean;
		public boolean striking;

		void pose(float[] from, float[] to, float e, float lean) {
			this.gripAngle = Mth.lerp(e, from[0], to[0]);
			this.gripX = Mth.lerp(e, from[1], to[1]);
			this.blade = Mth.lerp(e, from[2], to[2]);
			this.lean = lean;
		}

		// body space: pixels, y down, -z forward
		Vector3f grip() {
			return new Vector3f(this.gripX, 2.0F + GRIP_RADIUS * Mth.cos(this.gripAngle), -GRIP_RADIUS * Mth.sin(this.gripAngle));
		}

		Vector3f bladeDir() {
			return new Vector3f(0.0F, -Mth.cos(this.blade), -Mth.sin(this.blade));
		}
	}

	public static class Model extends /*? if >=26.1 {*/HumanoidModel<State>/*?} else {*//*PlayerModel<MossKnightEntity>*//*?}*/ {
		private static final float ARM_REACH = 9.0F;
		private static final float HAND_SPACING = 2.0F;
		//? if <26.1 {
		/*final State state = new State();
		*///?}

		public Model(ModelPart root) {
			super(root, /*? if >=26.1 {*/RenderTypes::entityTranslucent/*?} else {*//*false*//*?}*/);
		}

		//? if <26.1 {
		/*// the outer skin layer parts are siblings here rather than children, so they follow by copy
		@Override
		public void setupAnim(MossKnightEntity knight, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			this.headParts().forEach(ModelPart::resetPose);
			this.bodyParts().forEach(ModelPart::resetPose);
			super.setupAnim(knight, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			this.setupAnim(this.state);
			this.hat.copyFrom(this.head);
			this.jacket.copyFrom(this.body);
			this.rightSleeve.copyFrom(this.rightArm);
			this.leftSleeve.copyFrom(this.leftArm);
			this.rightPants.copyFrom(this.rightLeg);
			this.leftPants.copyFrom(this.leftLeg);
		}
		*///?}

		/*? if >=26.1 {*/@Override/*?}*/
		public void setupAnim(State state) {
			//? if >=26.1 {
			super.setupAnim(state);
			//?}
			float amount = Math.min(1.0F, state.walkAnimationSpeed * 3.0F);
			float phase = state.walkAnimationPos * 0.42F;
			float stride = 0.8F * amount;
			this.rightLeg.xRot = Mth.cos(phase) * stride;
			this.leftLeg.xRot = -Mth.cos(phase) * stride;
			this.rightLeg.yRot = 0.0F;
			this.leftLeg.yRot = 0.0F;
			if (state.striking) {
				this.rightLeg.xRot = Mth.lerp(0.7F, this.rightLeg.xRot, -0.45F);
				this.leftLeg.xRot = Mth.lerp(0.7F, this.leftLeg.xRot, 0.35F);
			}
			float spread = Math.max(Math.abs(this.rightLeg.xRot), Math.abs(this.leftLeg.xRot));
			float drop = 12.0F * (1 - Mth.cos(spread)) * 0.8F;
			for (ModelPart part : new ModelPart[] {this.head, this.body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg}) {
				part.y += drop;
			}
			this.body.zRot = Mth.sin(phase) * 0.07F * amount;
			this.body.yRot = Mth.sin(phase) * 0.1F * amount;
			// lean pivots at the hips, 12px below the body's origin
			this.body.xRot = state.lean;
			this.body.y += 12.0F * (1 - Mth.cos(state.lean));
			this.body.z -= 12.0F * Mth.sin(state.lean);
			this.head.y = this.body.y;
			this.head.z = this.body.z;
			this.head.xRot += state.lean * 0.5F;

			Quaternionf torso = new Quaternionf().rotationZYX(this.body.zRot, this.body.yRot, this.body.xRot);
			Vector3f neck = new Vector3f(this.body.x, this.body.y, this.body.z);
			Vector3f grip = state.grip();
			Vector3f along = state.bladeDir().mul(HAND_SPACING);
			reach(this.rightArm, torso, neck, new Vector3f(-5.0F, 2.0F, 0.0F), grip.add(along, new Vector3f()));
			reach(this.leftArm, torso, neck, new Vector3f(5.0F, 2.0F, 0.0F), grip.sub(along, new Vector3f()));
		}

		private static void reach(ModelPart arm, Quaternionf torso, Vector3f neck, Vector3f shoulder, Vector3f hand) {
			torso.transform(shoulder).add(neck);
			Vector3f dir = torso.transform(hand).add(neck).sub(shoulder);
			float slide = dir.length() - ARM_REACH;
			dir.normalize();
			arm.xRot = -(float) Math.acos(Mth.clamp(dir.y, -1.0F, 1.0F));
			arm.yRot = (float) Mth.atan2(-dir.x, -dir.z);
			arm.zRot = 0.0F;
			arm.x = shoulder.x + dir.x * slide;
			arm.y = shoulder.y + dir.y * slide;
			arm.z = shoulder.z + dir.z * slide;
		}
	}

	private static class GreatswordLayer extends RenderLayer</*? if >=26.1 {*/State/*?} else {*//*MossKnightEntity*//*?}*/, Model> {
		//? if >=26.1 {
		GreatswordLayer(KnightRenderer renderer) {
			super(renderer);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, State state, float yRot, float xRot) {
		//?} else {
		/*private final ItemInHandRenderer itemInHandRenderer;

		GreatswordLayer(KnightRenderer renderer, ItemInHandRenderer itemInHandRenderer) {
			super(renderer);
			this.itemInHandRenderer = itemInHandRenderer;
		}

		@Override
		public void render(PoseStack poseStack, MultiBufferSource buffers, int lightCoords, MossKnightEntity knight, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
			State state = this.getParentModel().state;
		*///?}
			if (state.greatsword.isEmpty() || state.isInvisible) {
				return;
			}
			poseStack.pushPose();
			Model model = this.getParentModel();
			//? if >=26.1 {
			model.root().translateAndRotate(poseStack);
			//?}
			model.body.translateAndRotate(poseStack);
			Vector3f grip = state.grip();
			poseStack.translate(grip.x / 16.0F, grip.y / 16.0F, grip.z / 16.0F);
			// sprite's blade runs corner to corner; rotate upright and centre the grip (texture px 3.5, 12.5) between the fists
			//? if >=26.3 {
			poseStack.rotate(Axis.XP, state.blade);
			poseStack.rotateDegrees(Axis.YP, 90.0F);
			poseStack.rotateDegrees(Axis.ZP, -135.0F);
			//?} else {
			/*poseStack.mulPose(Axis.XP.rotation(state.blade));
			poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
			poseStack.mulPose(Axis.ZP.rotationDegrees(-135.0F));
			*///?}
			poseStack.scale(GREATSWORD_SCALE, GREATSWORD_SCALE, GREATSWORD_SCALE);
			poseStack.translate(0.28F, 0.28F, 0.0F);
			//? if >=26.1 {
			state.greatsword.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
			//?} else {
			/*this.itemInHandRenderer.renderItem(knight, state.greatsword, ItemDisplayContext.NONE, false, poseStack, buffers, lightCoords);
			*///?}
			poseStack.popPose();
		}
	}
}
