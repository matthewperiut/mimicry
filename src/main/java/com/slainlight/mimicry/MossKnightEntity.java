package com.slainlight.mimicry;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
//? if >=26.1 {
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//?} else {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
*///?}
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// the greatsword is drawn by the renderer, not held as equipment
public class MossKnightEntity extends Monster {
	public static final int WINDUP = 15;
	public static final int STRIKE = 5;
	public static final int RECOVER = 30;
	public static final int SWING_LENGTH = WINDUP + STRIKE + RECOVER;
	private static final byte EVENT_SWING = 4;
	private static final double REACH = 2.9;

	private int swingTick = -1;
	private int cooldown;
	// set for keep knights, so CastlePiece can tell which posts are empty
	private long keep;
	private @Nullable BlockPos post;

	public MossKnightEntity(EntityType<? extends MossKnightEntity> type, Level level) {
		super(type, level);
		this.xpReward = 12;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 30.0)
			.add(Attributes.MOVEMENT_SPEED, 0.17)
			.add(Attributes.ATTACK_DAMAGE, 7.0)
			.add(Attributes.ARMOR, 8.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
			.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new GreatswordGoal());
		this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
		this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.7));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this, MossKnightEntity.class));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	public void guard(BlockPos post, long keep) {
		this.post = post;
		this.keep = keep;
		this.setHomeTo(post, 8);
		this.setPersistenceRequired();
	}

	public boolean guards(long keep, BlockPos post) {
		return this.keep == keep && post.equals(this.post);
	}

	public boolean hasPost() {
		return this.post != null;
	}

	//? if >=26.1 {
	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (this.post != null) {
			output.store("Post", BlockPos.CODEC, this.post);
			output.putLong("Keep", this.keep);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.post = input.read("Post", BlockPos.CODEC).orElse(null);
		this.keep = input.getLongOr("Keep", 0L);
	}
	//?} else {
	/*@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (this.post != null) {
			tag.put("Post", NbtUtils.writeBlockPos(this.post));
			tag.putLong("Keep", this.keep);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		//? if >=1.20.5 {
		this.post = NbtUtils.readBlockPos(tag, "Post").orElse(null);
		//?} else {
		/^this.post = tag.contains("Post") ? NbtUtils.readBlockPos(tag.getCompound("Post")) : null;
		^///?}
		this.keep = tag.getLong("Keep");
		// the home position isn't saved on these versions
		if (this.post != null) {
			this.setHomeTo(this.post, 8);
		}
	}
	*///?}

	private void startSwing() {
		this.swingTick = 0;
		this.level().broadcastEntityEvent(this, EVENT_SWING);
		this.playSound(Hollowmere.KNIGHT_WINDUP, 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
	}

	public boolean isSwinging() {
		return this.swingTick >= 0;
	}

	// ticks into the swing including partial tick, or -1 when not swinging; used by the renderer
	public float getSwingProgress(float partialTicks) {
		return this.swingTick < 0 ? -1.0F : Math.min(this.swingTick + partialTicks, SWING_LENGTH);
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == EVENT_SWING) {
			this.swingTick = 0;
		} else {
			super.handleEntityEvent(id);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.isDeadOrDying()) {
			this.swingTick = -1;
		} else if (this.swingTick >= 0) {
			if (!this.level().isClientSide() && this.swingTick == WINDUP + 2) {
				this.strike((ServerLevel) this.level());
			}
			if (++this.swingTick >= SWING_LENGTH) {
				this.swingTick = -1;
				this.cooldown = 16;
			}
		} else if (this.cooldown > 0) {
			this.cooldown--;
		}
	}

	private void strike(ServerLevel level) {
		float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
		Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
		float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
		this.playSound(Hollowmere.KNIGHT_SWING, 1.0F, 0.85F + this.random.nextFloat() * 0.15F);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(REACH, 1.0, REACH),
			e -> e != this && e.isAlive() && !(e instanceof MossKnightEntity))) {
			Vec3 offset = victim.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
			double distance = offset.length();
			if (distance > REACH + victim.getBbWidth() / 2 || distance > 0.5 && offset.dot(forward) / distance < 0.4) {
				continue;
			}
			if (victim./*? if >=1.21.2 {*/hurtServer(level, /*?} else {*//*hurt(*//*?}*/this.damageSources().mobAttack(this), damage)) {
				victim.knockback(0.8, -forward.x, -forward.z/*? if >=26.2 {*/, this.damageSources().mobAttack(this), damage/*?}*/);
			}
		}
		Vec3 impact = this.position().add(forward.scale(1.9));
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, impact.x, impact.y + 0.6, impact.z, 1, 0.0, 0.0, 0.0, 0.0);
		BlockState ground = level.getBlockState(BlockPos.containing(impact).below());
		if (!ground.isAir()) {
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), impact.x, impact.y + 0.1, impact.z, 24, 0.5, 0.1, 0.5, 0.15);
			level.playSound(null, impact.x, impact.y, impact.z, /*? if >=1.21 {*/SoundEvents.MACE_SMASH_GROUND/*?} else {*//*Mimicry.MACE_SMASH_GROUND*//*?}*/, this.getSoundSource(), 0.6F, 0.7F);
		}
	}

	@Override
	protected float nextStep() {
		return this.moveDist + 1.3F;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState blockState) {
		this.playSound(Hollowmere.KNIGHT_STEP, 0.7F, 0.85F + this.random.nextFloat() * 0.2F);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return Hollowmere.KNIGHT_AMBIENT;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 200;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return Hollowmere.KNIGHT_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return Hollowmere.KNIGHT_DEATH;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return !this.hasHome() && super.removeWhenFarAway(distSqr);
	}

	private class GreatswordGoal extends Goal {
		private int repath;

		GreatswordGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = MossKnightEntity.this.getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public boolean canContinueToUse() {
			return this.canUse() || MossKnightEntity.this.isSwinging();
		}

		@Override
		public void start() {
			MossKnightEntity.this.setAggressive(true);
			this.repath = 0;
		}

		@Override
		public void stop() {
			MossKnightEntity.this.setAggressive(false);
			MossKnightEntity.this.getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			MossKnightEntity knight = MossKnightEntity.this;
			LivingEntity target = knight.getTarget();
			if (knight.isSwinging()) {
				knight.getNavigation().stop();
				return;
			}
			if (target == null) {
				return;
			}
			knight.getLookControl().setLookAt(target, 20.0F, 30.0F);
			double distance = knight.distanceTo(target);
			if (distance < REACH - 0.4 && knight.cooldown == 0 && knight.hasLineOfSight(target)) {
				knight.getNavigation().stop();
				knight.setYBodyRot(knight.getYHeadRot());
				knight.startSwing();
			} else if (--this.repath <= 0) {
				this.repath = 10;
				knight.getNavigation().moveTo(target, 1.0);
			}
		}
	}

	//? if <1.20.5 {
	/*// 1.20.1 has no EntityType.Builder.eyeHeight
	@Override
	protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dimensions) {
		return dimensions.height * 1.62F / 1.95F;
	}
	*///?}
}
