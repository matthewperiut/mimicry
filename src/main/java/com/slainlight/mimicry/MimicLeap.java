package com.slainlight.mimicry;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MimicLeap {
	private MimicLeap() {
	}

	public static int chargeTicks(MimicEntity mimic) {
		return mimic.isKing() ? 30 : 8;
	}

	// server side, each tick: shrink just before a gap full size can't pass, grow back as soon as there's room
	static void squeeze(MimicEntity coffer) {
		Level level = coffer.level();
		boolean tight = tightAhead(coffer);
		if (!coffer.isSqueezed()) {
			// fallback for gaps the geometry check misses: following a path but blocked and not moving
			Path path = coffer.getNavigation().getPath();
			boolean pushing = path != null && !path.isDone() && coffer.horizontalCollision && coffer.getDeltaMovement().horizontalDistanceSqr() < 1.0E-3;
			coffer.stuckTicks = pushing ? coffer.stuckTicks + 1 : 0;
			if ((tight || coffer.stuckTicks > 10) && level.noCollision(coffer, box(coffer, true, coffer.position()))) {
				coffer.setSqueezed(true);
				coffer.stuckTicks = 0;
				coffer.shrunkTicks = 40; // minimum time shrunk so it doesn't flicker
			}
			return;
		}
		if (coffer.shrunkTicks > 0) {
			coffer.shrunkTicks--;
			return;
		}
		if (tight) {
			return;
		}
		for (double lift = 0.0; lift <= coffer.maxUpStep() + 1.0E-3; lift += 0.25) {
			if (level.noCollision(coffer, box(coffer, false, coffer.position().add(0.0, lift, 0.0)))) {
				if (lift > 0.0) {
					coffer.setPos(coffer.getX(), coffer.getY() + lift, coffer.getZ());
				}
				coffer.setSqueezed(false);
				return;
			}
		}
	}

	private static boolean tightAhead(MimicEntity coffer) {
		Path path = coffer.getNavigation().getPath();
		if (path == null || path.isDone() || coffer.getAttack() != MimicEntity.ATTACK_NONE) {
			return false;
		}
		for (int i = path.getNextNodeIndex(); i < Math.min(path.getNodeCount(), path.getNextNodeIndex() + 3); i++) {
			BlockPos node = path.getNodePos(i);
			if (Vec3.atBottomCenterOf(node).distanceToSqr(coffer.position()) > 2.5 * 2.5) {
				break;
			}
			if (!roomFor(coffer, node)) {
				return true;
			}
		}
		return false;
	}

	// tries the node centre and quarter-block offsets (wall sliding), ignoring anything low enough to step onto
	private static boolean roomFor(MimicEntity coffer, BlockPos node) {
		double floor = WalkNodeEvaluator.getFloorLevel(coffer.level(), node);
		double step = coffer.maxUpStep();
		for (double dx = -0.25; dx <= 0.26; dx += 0.25) {
			for (double dz = -0.25; dz <= 0.26; dz += 0.25) {
				AABB body = box(coffer, false, new Vec3(node.getX() + 0.5 + dx, floor + 0.01, node.getZ() + 0.5 + dz));
				if (coffer.level().noCollision(coffer, body.setMinY(body.minY + step))) {
					return true;
				}
			}
		}
		return false;
	}

	private static AABB box(MimicEntity coffer, boolean shrunk, Vec3 at) {
		EntityDimensions dimensions = coffer.getType().getDimensions();
		float scale = coffer.getScale();
		return (shrunk ? dimensions : dimensions.scale(MimicEntity.CHEST_WIDTH / dimensions./*? if >=1.20.5 {*/width()/*?} else {*//*width*//*?}*/ * scale, scale)).makeBoundingBox(at).deflate(1.0E-3);
	}

	// king paths as a 1x2x1 body since it can shrink to fit anywhere a zombie can
	static final class Navigation extends GroundPathNavigation {
		Navigation(Mob mob, Level level) {
			super(mob, level);
		}

		@Override
		protected PathFinder createPathFinder(int maxVisitedNodes) {
			this.nodeEvaluator = new WalkNodeEvaluator() {
				@Override
				public void prepare(PathNavigationRegion region, Mob mob) {
					super.prepare(region, mob);
					if (mob instanceof MimicEntity coffer && coffer.isKing()) {
						this.entityWidth = 1;
						this.entityHeight = 2;
						this.entityDepth = 1;
					}
				}
			};
			// larger node budget so it can find long detours
			return new PathFinder(this.nodeEvaluator, Math.max(maxVisitedNodes, 1024));
		}

		// vanilla steers wide mobs to the corner of a 2x2 node; these nodes are 1x1, so aim at the centre
		@Override
		public void tick() {
			super.tick();
			if (!this.isDone() && this.mob instanceof MimicEntity coffer && coffer.isKing()) {
				Vec3 centre = Vec3.atBottomCenterOf(this.path.getNextNodePos());
				this.mob.getMoveControl().setWantedPosition(centre.x, this.getGroundY(centre), centre.z, this.speedModifier);
			}
		}
	}

	static final class LeapGoal extends Goal {
		private final MimicEntity coffer;
		private Vec3 from = Vec3.ZERO;
		private Vec3 to = Vec3.ZERO;
		private double arc;
		private int flight;
		private int ticks;
		private int cooldown;
		private int repath;
		private float yaw;

		LeapGoal(MimicEntity coffer) {
			this.coffer = coffer;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = this.coffer.getTarget();
			return !this.coffer.isDormant() && target != null && target.isAlive() && this.coffer.canAttack(target);
		}

		@Override
		public boolean canContinueToUse() {
			byte attack = this.coffer.getAttack();
			return attack == MimicEntity.ATTACK_AIR || attack == MimicEntity.ATTACK_RECOVER || this.canUse();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			this.repath = 0;
		}

		@Override
		public void stop() {
			this.coffer.setAttack(MimicEntity.ATTACK_NONE);
			this.coffer.getNavigation().stop();
		}

		@Override
		public void tick() {
			LivingEntity target = this.coffer.getTarget();
			switch (this.coffer.getAttack()) {
				case MimicEntity.ATTACK_CHARGE -> this.charge(target);
				case MimicEntity.ATTACK_AIR -> this.fly();
				case MimicEntity.ATTACK_RECOVER -> {
					if (++this.ticks >= (this.coffer.isKing() ? 16 : 10)) {
						this.coffer.setAttack(MimicEntity.ATTACK_NONE);
					}
				}
				default -> this.stalk(target);
			}
		}

		private void stalk(LivingEntity target) {
			if (target == null) {
				return;
			}
			this.coffer.getLookControl().setLookAt(target, 30.0F, 30.0F);
			if (this.cooldown > 0) {
				this.cooldown--;
			}
			boolean king = this.coffer.isKing();
			double gap = this.coffer.distanceTo(target) - (this.coffer.getBbWidth() + target.getBbWidth()) / 2.0;
			if (this.cooldown == 0 && this.coffer.onGround() && gap < (king ? 9.0 : 2.0) && Math.abs(target.getY() - this.coffer.getY()) < (king ? 4.0 : 1.5)
				&& this.coffer.getSensing().hasLineOfSight(target) && this.plan(target)) {
				this.coffer.getNavigation().stop();
				this.coffer.setAttack(MimicEntity.ATTACK_CHARGE);
				this.ticks = 0;
				this.coffer.playSound(Mimicry.MIMIC_GROWL, king ? 2.0F : 0.8F, this.coffer.getVoicePitch() * (king ? 0.8F : 1.1F));
			} else if (--this.repath <= 0) {
				this.repath = 10;
				this.coffer.getNavigation().moveTo(target, king ? 1.1 : 1.0);
			}
		}

		private void charge(LivingEntity target) {
			if (target == null) {
				this.coffer.setAttack(MimicEntity.ATTACK_NONE);
				return;
			}
			this.coffer.getNavigation().stop();
			float toward = (float) (Mth.atan2(target.getZ() - this.coffer.getZ(), target.getX() - this.coffer.getX()) * Mth.RAD_TO_DEG) - 90.0F;
			this.face(Mth.approachDegrees(this.coffer.getYRot(), toward, 20.0F));
			if (++this.ticks >= chargeTicks(this.coffer)) {
				if (this.plan(target)) {
					this.leap();
				} else {
					this.coffer.setAttack(MimicEntity.ATTACK_NONE);
					this.cooldown = 10;
				}
			}
		}

		private boolean plan(LivingEntity target) {
			boolean king = this.coffer.isKing();
			this.from = this.coffer.position();
			this.to = target.position();
			if (!king) {
				Vec3 back = this.from.subtract(this.to).multiply(1.0, 0.0, 1.0);
				double stop = (this.coffer.getBbWidth() + target.getBbWidth()) / 2.0 * 0.8;
				this.to = back.length() > stop ? this.to.add(back.normalize().scale(stop)) : this.from;
			}
			double reach = this.to.subtract(this.from).horizontalDistance();
			this.flight = king ? Mth.clamp((int) (10 + reach * 1.3), 12, 26) : Mth.clamp((int) (5 + reach), 6, 9);
			for (this.arc = king ? 1.2 + reach * 0.22 : 0.45 + reach * 0.1; this.arc >= 0.4; this.arc -= 0.35) {
				if (this.clearArc()) {
					return true;
				}
			}
			return false;
		}

		private boolean clearArc() {
			AABB body = this.coffer.getBoundingBox().deflate(0.05);
			for (int i = 1; i <= 12; i++) {
				AABB at = body.move(this.arcAt(i / 12.0).subtract(this.from));
				if (!this.coffer.level().noCollision(this.coffer, i == 12 ? at.setMinY(at.minY + this.coffer.maxUpStep()) : at)) {
					return false;
				}
			}
			return true;
		}

		private void leap() {
			boolean king = this.coffer.isKing();
			this.yaw = this.coffer.getYRot();
			this.ticks = 0;
			this.coffer.setFlight(this.flight);
			this.coffer.setAttack(MimicEntity.ATTACK_AIR);
			if (king) {
				this.coffer.playSound(SoundEvents.GOAT_LONG_JUMP, 2.0F, 0.6F);
			} else {
				this.coffer.playSound(Mimicry.MIMIC_HOP, 0.8F, this.coffer.getVoicePitch());
			}
		}

		private Vec3 arcAt(double t) {
			return new Vec3(Mth.lerp(t, this.from.x, this.to.x), Mth.lerp(t, this.from.y, this.to.y) + 4.0 * this.arc * t * (1.0 - t), Mth.lerp(t, this.from.z, this.to.z));
		}

		private void fly() {
			this.ticks++;
			this.coffer.resetFallDistance();
			if (this.ticks > 1 && this.coffer.onGround() || this.ticks > this.flight + 60) {
				this.land();
				return;
			}
			if (this.ticks <= this.flight) {
				this.coffer.setDeltaMovement(this.arcAt(this.ticks / (double) this.flight).subtract(this.arcAt((this.ticks - 1) / (double) this.flight)));
			}
			this.face(this.yaw);
		}

		private void land() {
			this.coffer.setDeltaMovement(Vec3.ZERO);
			this.coffer.setAttack(MimicEntity.ATTACK_RECOVER);
			this.ticks = 0;
			if (this.coffer.isKing()) {
				this.cooldown = 30 + this.coffer.getRandom().nextInt(30);
				this.crash((ServerLevel) this.coffer.level());
			} else {
				this.cooldown = 20 + this.coffer.getRandom().nextInt(10);
				this.snap((ServerLevel) this.coffer.level());
			}
		}

		private void snap(ServerLevel level) {
			LivingEntity target = this.coffer.getTarget();
			if (target != null && target.isAlive()) {
				double reach = (this.coffer.getBbWidth() + target.getBbWidth()) / 2.0 + 0.7;
				double rise = target.getY() - this.coffer.getY();
				if (this.coffer.distanceToSqr(target.getX(), this.coffer.getY(), target.getZ()) <= reach * reach && rise > -1.0 && rise < 1.5) {
					this.coffer.doHurtTarget(/*? if >=1.21.2 {*/level, /*?}*/target);
					return;
				}
			}
			this.coffer.playSound(Mimicry.MIMIC_CHOMP, 1.0F, this.coffer.getVoicePitch());
		}

		private void crash(ServerLevel level) {
			double x = this.coffer.getX();
			double y = this.coffer.getY();
			double z = this.coffer.getZ();
			double width = this.coffer.getBbWidth();
			double radius = width / 2.0 + 1.25;
			BlockState ground = this.coffer.getBlockStateOn();
			if (!ground.isAir()) {
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), x, y + 0.1, z, 120, radius * 0.4, 0.1, radius * 0.4, 0.2);
			}
			for (int i = 0; i < 28; i++) {
				double angle = i * Mth.TWO_PI / 28;
				double dx = Math.cos(angle);
				double dz = Math.sin(angle);
				level.sendParticles(ParticleTypes.POOF, x + dx * width * 0.5, y + 0.15, z + dz * width * 0.5, 0, dx, 0.02, dz, 0.35);
			}
			level.playSound(null, x, y, z, /*? if >=1.21 {*/SoundEvents.MACE_SMASH_GROUND_HEAVY/*?} else {*//*Mimicry.MACE_SMASH_GROUND_HEAVY*//*?}*/, this.coffer.getSoundSource(), 2.0F, 0.75F);
			this.coffer.playSound(Mimicry.MIMIC_CHOMP, 1.5F, this.coffer.getVoicePitch());

			DamageSource source = this.coffer.damageSources().mobAttack(this.coffer);
			float damage = (float) this.coffer.getAttributeValue(Attributes.ATTACK_DAMAGE);
			AABB area = new AABB(x - radius, y - 0.5, z - radius, x + radius, y + this.coffer.getBbHeight(), z + radius);
			for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, this::crushes)) {
				double reach = radius + victim.getBbWidth() / 2.0;
				if (victim.distanceToSqr(x, victim.getY(), z) <= reach * reach && victim./*? if >=1.21.2 {*/hurtServer(level, /*?} else {*//*hurt(*//*?}*/source, damage)) {
					victim.knockback(1.2, x - victim.getX(), z - victim.getZ()/*? if >=26.2 {*/, source, damage/*?}*/);
					//? if >=1.21 {
					EnchantmentHelper.doPostAttackEffects(level, victim, source);
					//?} else {
					/*this.coffer.doEnchantDamageEffects(this.coffer, victim);
					*///?}
				}
			}
		}

		private boolean crushes(LivingEntity victim) {
			if (victim == this.coffer || !victim.isAlive() || this.coffer.isAlliedTo(victim)) {
				return false;
			}
			LivingEntity owner = this.coffer.getOwner();
			if (owner != null) {
				return victim != owner && !(victim instanceof OwnableEntity pet && pet.getOwner() == owner);
			}
			return !(victim instanceof MimicEntity) && !(victim instanceof MossKnightEntity);
		}

		private void face(float yaw) {
			this.coffer.setYRot(yaw);
			this.coffer.setYBodyRot(yaw);
			this.coffer.setYHeadRot(yaw);
		}
	}
}
