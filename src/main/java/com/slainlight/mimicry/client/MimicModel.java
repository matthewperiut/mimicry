package com.slainlight.mimicry.client;

//? if <26.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.slainlight.mimicry.MimicEntity;
import net.minecraft.client.renderer.RenderType;
*///?}
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

// shell cubes and UVs match ChestModel so it takes the vanilla chest texture; each render pass hides the other's parts
public class MimicModel extends EntityModel</*? if >=26.1 {*/MimicRenderer.State/*?} else {*//*MimicEntity*//*?}*/> {
	//? if <26.1 {
	/*private final ModelPart root;
	*///?}
	private final ModelPart chest;
	private final ModelPart lid;
	private final ModelPart lock;
	private final ModelPart tongue;
	private final ModelPart tip;
	private final ModelPart tipEnd;

	public MimicModel(ModelPart root, boolean flesh) {
		//? if >=26.1 {
		super(root);
		//?} else {
		/*super(RenderType::entityCutout);
		this.root = root;
		*///?}
		this.chest = root.getChild("chest");
		this.lid = this.chest.getChild("lid");
		this.lock = this.chest.getChild("lock");
		this.tongue = this.chest.getChild("tongue");
		this.tip = this.tongue.getChild("tip");
		this.tipEnd = this.tip.getChild("tip_end");
		this.chest.getChild("bottom").visible = !flesh;
		this.lock.visible = !flesh;
		this.lid.skipDraw = flesh;
		this.chest.getChild("jaw").visible = flesh;
		this.lid.getChild("palate").visible = flesh;
		this.tongue.visible = flesh;
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		// 180° about X maps block-entity space (y up, +z front) to entity model space (y down, -z front)
		PartDefinition chest = mesh.getRoot().addOrReplaceChild("chest", CubeListBuilder.create(), PartPose.offsetAndRotation(-8.0F, 24.0F, 8.0F, Mth.PI, 0.0F, 0.0F));
		chest.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 19).addBox(1.0F, 0.0F, 1.0F, 14.0F, 10.0F, 14.0F), PartPose.ZERO);
		PartDefinition lid = chest.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(1.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F), PartPose.offset(0.0F, 9.0F, 1.0F));
		chest.addOrReplaceChild("lock", CubeListBuilder.create().texOffs(0, 0).addBox(7.0F, -2.0F, 14.0F, 2.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 9.0F, 1.0F));

		// teeth are inset from the gum edges and off each other's planes to avoid z-fighting
		CubeListBuilder jaw = CubeListBuilder.create().texOffs(0, 0).addBox(2.0F, 10.1F, 2.0F, 12.0F, 0.15F, 12.0F).texOffs(24, 28);
		for (int i = 0; i < 6; i++) {
			if (i != 2 && i != 3) {
				jaw.addBox(2.5F + i * 2, 10.0F, 12.8F, 1.0F, 1.5F, 1.0F);
			}
		}
		for (int i = 0; i < 3; i++) {
			jaw.addBox(2.2F, 10.0F, 4.5F + i * 3, 1.0F, 1.5F, 1.0F);
			jaw.addBox(12.8F, 10.0F, 4.5F + i * 3, 1.0F, 1.5F, 1.0F);
		}
		chest.addOrReplaceChild("jaw", jaw, PartPose.ZERO);

		CubeListBuilder palate = CubeListBuilder.create().texOffs(0, 14).addBox(2.0F, -0.25F, 1.0F, 12.0F, 0.15F, 12.0F).texOffs(24, 28);
		for (int i = 0; i < 5; i++) {
			palate.addBox(3.5F + i * 2, -1.5F, 11.5F, 1.0F, 1.5F, 1.0F);
		}
		for (int i = 0; i < 3; i++) {
			palate.addBox(2.5F, -1.5F, 3.0F + i * 3, 1.0F, 1.5F, 1.0F);
			palate.addBox(12.5F, -1.5F, 3.0F + i * 3, 1.0F, 1.5F, 1.0F);
		}
		palate.texOffs(32, 28).addBox(3.5F, -0.75F, 5.0F, 3.0F, 0.5F, 3.0F).addBox(9.5F, -0.75F, 5.0F, 3.0F, 0.5F, 3.0F);
		lid.addOrReplaceChild("palate", palate, PartPose.ZERO);

		// tip segments hinge on their top edge; a bottom hinge opens a gap on top of the curl
		chest.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(0, 28).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 1.0F, 7.0F), PartPose.offset(8.0F, 10.3F, 3.0F))
			.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(6, 33).addBox(-2.0F, -1.0F, 0.0F, 4.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 1.0F, 7.0F))
			.addOrReplaceChild("tip_end", CubeListBuilder.create().texOffs(6, 33).addBox(-2.0F, -1.0F, 0.0F, 4.0F, 1.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 1.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	//? if <26.1 {
	/*// posed by MimicRenderer from its State, since this hook gets no partial tick
	@Override
	public void setupAnim(MimicEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}

	//? if >=1.21 {
	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
		this.root.render(poseStack, buffer, light, overlay, color);
	}
	//?} else {
	/^@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha) {
		this.root.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
	}
	^///?}
	*///?}

	/*? if >=26.1 {*/@Override/*?}*/
	public void setupAnim(MimicRenderer.State state) {
		//? if >=26.1 {
		super.setupAnim(state);
		//?} else {
		/*this.root.getAllParts().forEach(ModelPart::resetPose);
		*///?}
		float hop = Math.abs(Mth.sin(state.walkAnimationPos * 0.42F)) * Math.min(state.walkAnimationSpeed, 1.0F) * 4.0F;
		this.chest.y -= hop;
		float open = Math.min(1.0F, state.mouth + hop * 0.08F);
		float hurt = state.dormant || !state.tame ? 0.0F : 1.0F - Mth.clamp(state.health, 0.0F, 1.0F);
		float out = state.tame ? Mth.clamp((state.mouth - 0.03F) * 12.0F, 0.0F, 1.0F) : 0.0F;
		float slide = Math.max(out, Math.min(1.0F, hurt * 3.0F));
		// 5.2 rather than 5 so the curled tip doesn't cut into the chest front
		this.tongue.z += 5.2F * slide;
		this.tip.xRot = this.tipEnd.xRot = slide * slide * (0.06F * out + 0.65F * hurt);
		if (!state.tame) {
			this.tongue.xRot = -0.4F * open * (0.5F + 0.5F * Mth.sin(state.ageInTicks * 0.5F));
		}
		this.lid.xRot = -Math.max(Math.max(open * 1.75F, state.lid * Mth.HALF_PI), 0.34F * slide);
		this.lock.xRot = this.lid.xRot;
	}
}
