package com.slainlight.mimicry.client;

import com.slainlight.mimicry.MimicEntity;
import com.slainlight.mimicry.Mimicry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.SpecialDates;

public class MimicRenderer extends MobRenderer<MimicEntity, MimicRenderer.State, MimicModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Mimicry.id("mimic"), "main");
	private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/entity/chest/normal.png");
	private static final Identifier CHRISTMAS_CHEST = Identifier.withDefaultNamespace("textures/entity/chest/christmas.png");
	private static final Identifier FLESH = Mimicry.id("textures/entity/mimic/flesh.png");
	private static final Identifier EYES = Mimicry.id("textures/entity/mimic/eyes.png");

	public MimicRenderer(EntityRendererProvider.Context context) {
		super(context, new MimicModel(context.bakeLayer(LAYER), false), 0.5F);
		MimicModel flesh = new MimicModel(context.bakeLayer(LAYER), true);
		this.addLayer(new RenderLayer<>(this) {
			@Override
			public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, State state, float yRot, float xRot) {
				coloredCutoutModelCopyLayerRender(flesh, FLESH, poseStack, submitNodeCollector, lightCoords, state, -1, 1);
				if (!state.isInvisible) {
					submitNodeCollector.order(2)
						.submitModel(flesh, state, poseStack, RenderTypes.eyes(EYES), lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
				}
			}
		});
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return SpecialDates.isExtendedChristmas() ? CHRISTMAS_CHEST : CHEST;
	}

	@Override
	protected float getShadowRadius(State state) {
		return state.closed ? 0.0F : super.getShadowRadius(state);
	}

	private static final float HALF = 7.0F / 16.0F;

	// pose space: origin at the feet, y down, -z forward
	@Override
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

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(MimicEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
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

	public static class State extends LivingEntityRenderState {
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
