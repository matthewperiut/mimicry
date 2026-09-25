package com.slainlight.mimicry.client;

import com.slainlight.mimicry.MimicEntity;
import com.slainlight.mimicry.Mimicry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
//? if >=26.1 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.SpecialDates;
//?} else {
/*import java.time.Month;
import java.time.MonthDay;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
*///?}
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class MimicRenderer extends MobRenderer<MimicEntity, /*? if >=26.1 {*/MimicRenderer.State, /*?}*/MimicModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Mimicry.id("mimic"), "main");
	private static final Identifier CHEST = /*? if >=1.21 {*/Identifier.withDefaultNamespace/*?} else {*//*new Identifier*//*?}*/("textures/entity/chest/normal.png");
	private static final Identifier CHRISTMAS_CHEST = /*? if >=1.21 {*/Identifier.withDefaultNamespace/*?} else {*//*new Identifier*//*?}*/("textures/entity/chest/christmas.png");
	private static final Identifier FLESH = Mimicry.id("textures/entity/mimic/flesh.png");
	private static final Identifier EYES = Mimicry.id("textures/entity/mimic/eyes.png");

	public MimicRenderer(EntityRendererProvider.Context context) {
		super(context, new MimicModel(context.bakeLayer(LAYER), false), 0.5F);
		MimicModel flesh = new MimicModel(context.bakeLayer(LAYER), true);
		//? if >=26.1 {
		this.addLayer(new RenderLayer<>(this) {
			@Override
			public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, State state, float yRot, float xRot) {
				coloredCutoutModelCopyLayerRender(flesh, FLESH, poseStack, submitNodeCollector, lightCoords, state, -1, 1);
				if (!state.isInvisible) {
					submitNodeCollector.order(2)
						.submitModel(flesh, state, poseStack, RenderTypes.eyes(EYES), lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor/*? if <26.3 {*//*, null*//*?}*/);
				}
			}
		});
		//?} else {
		/*this.addLayer(new RenderLayer<>(this) {
			@Override
			public void render(PoseStack poseStack, MultiBufferSource buffers, int light, MimicEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				if (!entity.isInvisible()) {
					flesh.setupAnim(MimicRenderer.this.state);
					flesh.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityCutout(FLESH)), light, LivingEntityRenderer.getOverlayCoords(entity, 0.0F)/^? if <1.21 {^//^, 1.0F, 1.0F, 1.0F, 1.0F^//^?}^/);
					flesh.renderToBuffer(poseStack, buffers.getBuffer(RenderType.eyes(EYES)), light, OverlayTexture.NO_OVERLAY/^? if <1.21 {^//^, 1.0F, 1.0F, 1.0F, 1.0F^//^?}^/);
				}
			}
		});
		*///?}
	}

	@Override
	public Identifier getTextureLocation(/*? if >=26.1 {*/State state/*?} else {*//*MimicEntity entity*//*?}*/) {
		//? if >=26.1 {
		return SpecialDates.isExtendedChristmas() ? CHRISTMAS_CHEST : CHEST;
		//?} else {
		/*MonthDay day = MonthDay.now();
		return day.getMonth() == Month.DECEMBER && day.getDayOfMonth() >= 24 && day.getDayOfMonth() <= 26 ? CHRISTMAS_CHEST : CHEST;
		*///?}
	}

	//? if >=26.1 {
	@Override
	protected float getShadowRadius(State state) {
		return state.closed ? 0.0F : super.getShadowRadius(state);
	}
	//?} else {
	/*private final State state = new State();

	@Override
	public void render(MimicEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
		this.extractRenderState(entity, this.state, partialTicks);
		this.model.setupAnim(this.state);
		//? if <1.20.5 {
		/^this.shadowRadius = this.state.closed ? 0.0F : 0.5F * this.state.scale;
		^///?}
		super.render(entity, yaw, partialTicks, poseStack, buffers, light);
	}

	//? if >=1.20.5 {
	@Override
	protected float getShadowRadius(MimicEntity entity) {
		return entity.isDormant() || entity.isInSittingPose() ? 0.0F : super.getShadowRadius(entity);
	}
	//?}

	@Override
	protected void scale(MimicEntity entity, PoseStack poseStack, float partialTicks) {
		//? if <1.20.5 {
		/^// later versions apply getScale() in LivingEntityRenderer
		poseStack.scale(this.state.scale, this.state.scale, this.state.scale);
		^///?}
		this.scale(this.state, poseStack);
	}
	*///?}

	private static final float HALF = 7.0F / 16.0F;

	// pose space: origin at the feet, y down, -z forward
	/*? if >=26.1 {*/@Override/*?}*/
	protected void scale(State state, PoseStack poseStack) {
		float crouch = state.charge;
		float tremble = state.king ? 0.015F * crouch * Mth.sin(state.ageInTicks * 2.7F) : 0.0F;
		float squash = 1.0F - 0.1F * crouch + tremble + 0.1F * state.lift - 0.12F * state.landing;
		float spread = 1.0F + 0.05F * crouch - 0.05F * state.lift + 0.08F * state.landing;
		float shrink = Mth.lerp(state.squeeze, 1.0F, 1.0F / state.scale);
		float edge = HALF * spread * shrink;
		float tilt = (state.king ? 0.28F : 0.15F) * crouch - 0.45F * state.lean;
		poseStack.rotateAround(Axis.XP.rotation(-tilt), 0.0F, 0.0F, -edge);
		// rotate about the centre, raised so the lowest edge stays on the ground
		if (state.pitch != 0.0F) {
			float lift = HALF * (Math.abs(Mth.cos(state.pitch)) + Math.abs(Mth.sin(state.pitch)) - 1.0F);
			poseStack.translate(0.0F, -lift, 0.0F);
			poseStack.rotateAround(Axis.XP.rotation(-state.pitch), 0.0F, -HALF, 0.0F);
		}
		poseStack.scale(shrink * spread, shrink * squash, shrink * spread);
	}

	//? if >=26.1 {
	@Override
	public State createRenderState() {
		return new State();
	}
	//?}

	/*? if >=26.1 {*/@Override/*?}*/
	public void extractRenderState(MimicEntity entity, State state, float partialTicks) {
		//? if >=26.1 {
		super.extractRenderState(entity, state, partialTicks);
		//?} else {
		/*boolean walking = !entity.isPassenger() && entity.isAlive();
		state.ageInTicks = entity.tickCount + partialTicks;
		state.walkAnimationPos = walking ? entity.walkAnimation.position(partialTicks) : 0.0F;
		state.walkAnimationSpeed = walking ? entity.walkAnimation.speed(partialTicks) : 0.0F;
		state.scale = entity.getScale();
		state.isInvisible = entity.isInvisible();
		*///?}
		state.mouth = entity.getMouthOpen(partialTicks);
		state.lid = entity.getLidOpen(partialTicks);
		state.tame = entity.isTame();
		state.dormant = entity.isDormant();
		state.closed = entity.isDormant() || entity.isInSittingPose();
		state.health = entity.getHealth() / entity.getMaxHealth();
		state.king = entity.isKing();
		state.charge = entity.getCharge(partialTicks);
		state.lean = entity.getLean(partialTicks);
		state.squeeze = entity.getSqueeze(partialTicks);
		state.lift = entity.getLift(partialTicks);
		state.landing = entity.getLanding(partialTicks);
		state.mouth = Math.max(state.mouth, entity.getFlap(partialTicks));
		state.pitch = entity.getFlip(partialTicks);
		if (entity.getAttack() != MimicEntity.ATTACK_NONE) {
			state.walkAnimationSpeed = 0.0F;
		}
	}

	public static class State/*? if >=26.1 {*/ extends LivingEntityRenderState/*?}*/ {
		//? if <26.1 {
		/*public float ageInTicks;
		public float walkAnimationPos;
		public float walkAnimationSpeed;
		public float scale = 1.0F;
		public boolean isInvisible;
		*///?}
		public float mouth;
		public float lid;
		public boolean tame;
		public boolean dormant;
		public boolean closed;
		public float health = 1.0F;
		public boolean king;
		public float charge;
		public float lean;
		public float squeeze;
		public float lift;
		public float landing;
		// radians, positive = nose up
		public float pitch;
	}
}
