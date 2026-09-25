package com.slainlight.mimicry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MimicEntity extends TamableAnimal implements InventoryCarrier {
	private static final EntityDataAccessor<Boolean> DATA_DORMANT = SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_OPEN = SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_KING = SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_SQUEEZED = SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);
	// leap duration in ticks, synced so the client can time the somersault
	private static final EntityDataAccessor<Integer> DATA_FLIGHT = SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> DATA_ATTACK = SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BYTE);
	public static final byte ATTACK_NONE = 0;
	public static final byte ATTACK_CHARGE = 1;
	public static final byte ATTACK_AIR = 2;
	public static final byte ATTACK_RECOVER = 3;
	private static final byte EVENT_CHOMP = 4;
	private static final int DORMANT_AFTER_TICKS = 200;
	private static final double VACUUM_RANGE = 6.0;
	// width of the chest model; the base hitbox is narrower so it fits past an open door
	static final float CHEST_WIDTH = 0.875F;

	private final SimpleContainer inventory = new SimpleContainer(27) {
		private int viewers;

		@Override
		public boolean stillValid(Player player) {
			return MimicEntity.this.isAlive() && player.closerThan(MimicEntity.this, 8.0);
		}

		@Override
		public void startOpen(ContainerUser user) {
			if (this.viewers++ == 0) {
				MimicEntity.this.entityData.set(DATA_OPEN, true);
				MimicEntity.this.navigation.stop();
				MimicEntity.this.playSound(SoundEvents.CHEST_OPEN, 0.5F, 0.9F + MimicEntity.this.random.nextFloat() * 0.1F);
			}
		}

		@Override
		public void stopOpen(ContainerUser user) {
			if (this.viewers > 0 && --this.viewers == 0) {
				MimicEntity.this.entityData.set(DATA_OPEN, false);
				MimicEntity.this.playSound(SoundEvents.CHEST_CLOSE, 0.5F, 0.9F + MimicEntity.this.random.nextFloat() * 0.1F);
			}
		}
	};
	private @Nullable ServerBossEvent bossEvent;
	private int idleTicks;
	private int burpTicks;
	private float mouth;
	private float mouthO;
	private float lid;
	private float lidO;
	// NaN when not sitting
	private float sitYaw = Float.NaN;
	private int chompTicks;
	int stuckTicks;
	int shrunkTicks;
	private int attackTicks;
	private float lean;
	private float leanO;
	private float squeeze;
	private float squeezeO;
	private float flap;
	private float flapO;
	private float flapSpeed;
	// radians, positive is nose up
	private float flip;
	private float flipO;
	private float landedFlip;
	private float lift;
	private float liftO;
	private float landing;
	private float landingO;

	public MimicEntity(EntityType<? extends MimicEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.MOVEMENT_SPEED, 0.27)
			.add(Attributes.ARMOR, 2.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
			.add(Attributes.FOLLOW_RANGE, 20.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		this.goalSelector.addGoal(3, new MimicLeap.LeapGoal(this));
		this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.1, 6.0F, 2.0F));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
		this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(4, new NonTameRandomTargetGoal<>(this, Player.class, false, null));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_DORMANT, false);
		entityData.define(DATA_OPEN, false);
		entityData.define(DATA_KING, false);
		entityData.define(DATA_SQUEEZED, false);
		entityData.define(DATA_ATTACK, ATTACK_NONE);
		entityData.define(DATA_FLIGHT, 20);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new MimicLeap.Navigation(this, level);
	}

	public boolean isKing() {
		return this.entityData.get(DATA_KING);
	}

	public boolean isSqueezed() {
		return this.entityData.get(DATA_SQUEEZED);
	}

	void setSqueezed(boolean squeezed) {
		this.entityData.set(DATA_SQUEEZED, squeezed);
	}

	public byte getAttack() {
		return this.entityData.get(DATA_ATTACK);
	}

	void setAttack(byte attack) {
		this.entityData.set(DATA_ATTACK, attack);
	}

	void setFlight(int ticks) {
		this.entityData.set(DATA_FLIGHT, ticks);
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		EntityDimensions dimensions = super.getDefaultDimensions(pose);
		if (this.isSqueezed()) {
			// cancel the SCALE attribute so it's back to normal mimic size
			return dimensions.scale(1.0F / this.getScale());
		}
		// king uses the full chest width and squeezes to get through doors instead
		return this.isKing() ? dimensions.scale(CHEST_WIDTH / dimensions.width(), 1.0F) : dimensions;
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_SQUEEZED.equals(accessor) || DATA_KING.equals(accessor)) {
			this.refreshDimensions();
		} else if (DATA_ATTACK.equals(accessor) && this.level().isClientSide()) {
			this.attackTicks = 0;
			float heft = this.isKing() ? 1.0F : 0.5F;
			if (this.getAttack() == ATTACK_AIR) {
				if (this.isKing()) {
					this.flapSpeed += 0.45F;
				}
				this.lift = heft;
			} else if (this.getAttack() == ATTACK_RECOVER) {
				this.flapSpeed = -0.6F;
				this.landing = heft;
				this.landedFlip = this.flip;
			}
		}
	}

	public boolean isDormant() {
		return this.entityData.get(DATA_DORMANT);
	}

	public void setDormant(boolean dormant) {
		this.entityData.set(DATA_DORMANT, dormant);
	}

	public void crown() {
		this.getAttribute(Attributes.SCALE).setBaseValue(1.7);
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(60.0);
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(11.0); // 5.5 hearts unarmoured
		this.getAttribute(Attributes.ARMOR).setBaseValue(6.0);
		this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
		this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(32.0);
		this.setHealth(this.getMaxHealth());
		this.setCustomName(Component.translatable("entity.mimicry.kings_coffer"));
		this.bossEvent = new ServerBossEvent(this.getUUID(), this.getDisplayName(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
		this.entityData.set(DATA_KING, true);
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		if (this.bossEvent != null && !this.isTame()) {
			this.bossEvent.addPlayer(player);
		}
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		if (this.bossEvent != null) {
			this.bossEvent.removePlayer(player);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.bossEvent != null) {
			MimicLeap.squeeze(this);
			this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
			if (this.isTame()) {
				this.bossEvent.removeAllPlayers();
			}
		}
	}

	@Override
	public SimpleContainer getInventory() {
		return this.inventory;
	}

	private void goDormant() {
		this.setDormant(true);
		this.getNavigation().stop();
		this.setDeltaMovement(Vec3.ZERO);
		float yaw = Math.round(this.getYRot() / 90.0F) * 90.0F;
		double x = Mth.floor(this.getX()) + 0.5;
		double z = Mth.floor(this.getZ()) + 0.5;
		if (this.level().noCollision(this, this.getBoundingBox().move(x - this.getX(), 0.0, z - this.getZ()))) {
			this.snapTo(x, this.getY(), z, yaw, 0.0F);
		} else {
			this.setYRot(yaw);
		}
		this.setYBodyRot(yaw);
		this.setYHeadRot(yaw);
	}

	public void wake(@Nullable LivingEntity target) {
		this.setDormant(false);
		this.idleTicks = 0;
		if (target != null) {
			this.setTarget(target);
		}
		this.playSound(Mimicry.MIMIC_REVEAL, 1.0F, this.getVoicePitch());
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() / (float) Math.sqrt(this.getScale());
	}

	@Override
	protected boolean isImmobile() {
		return super.isImmobile() || this.isDormant() || this.isOpenAsChest();
	}

	public boolean isOpenAsChest() {
		return this.entityData.get(DATA_OPEN);
	}

	@Override
	public void makeStuckInBlock(BlockState state, Vec3 speedMultiplier) {
		if (!state.is(net.minecraft.world.level.block.Blocks.COBWEB)) {
			super.makeStuckInBlock(state, speedMultiplier);
		}
	}

	@Override
	public boolean isPushable() {
		return !this.isDormant() && super.isPushable();
	}

	@Override
	public void aiStep() {
		super.aiStep();
		// stop slurping once dead, or it eats its own death drops
		if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) {
			return;
		}

		if (this.isTame()) {
			if (this.tickCount % 4 == 0) {
				this.slurpItems(level);
			}
			if (this.burpTicks > 0 && --this.burpTicks == 0) {
				this.playSound(Mimicry.MIMIC_BURP, 1.0F, this.getVoicePitch());
			}
		} else if (this.isDormant()) {
			if (this.random.nextInt(600) == 0) {
				this.playSound(Mimicry.MIMIC_BREATHE, 0.6F, this.getVoicePitch());
			}
			Player victim = level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), 2.0, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
			if (victim != null) {
				this.wake(victim);
			}
		} else if (this.getTarget() == null && this.onGround()) {
			if (++this.idleTicks > DORMANT_AFTER_TICKS) {
				this.goDormant();
			}
		} else {
			this.idleTicks = 0;
		}
	}

	private void slurpItems(ServerLevel level) {
		Vec3 mouthPos = this.position().add(0.0, 0.5, 0.0);
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(VACUUM_RANGE), this::wantsToEat)) {
			if (item.distanceToSqr(mouthPos) < 1.5) {
				int before = item.getItem().getCount();
				InventoryCarrier.pickUpItem(level, this, this, item);
				if (item.isRemoved() || item.getItem().getCount() < before) {
					this.level().broadcastEntityEvent(this, EVENT_CHOMP);
					this.playSound(Mimicry.MIMIC_GULP, 0.8F, this.getVoicePitch());
					if (this.burpTicks == 0 && this.random.nextInt(8) == 0) {
						this.burpTicks = 25;
					}
				}
			} else {
				Vec3 pull = mouthPos.subtract(item.position()).normalize().scale(0.12);
				item.push(pull.x, pull.y + 0.02, pull.z);
			}
		}
	}

	private boolean wantsToEat(ItemEntity item) {
		return !item.hasPickUpDelay() && !(item.getOwner() instanceof LivingEntity thrower && this.isOwnedBy(thrower))
			&& this.inventory.canAddItem(item.getItem());
	}

	@Override
	public boolean wantsToPickUp(ServerLevel level, ItemStack itemStack) {
		return this.inventory.canAddItem(itemStack);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		boolean treat = stack.is(Mimicry.MIMIC_FOOD);
		if (!(this.level() instanceof ServerLevel level)) {
			return treat && !this.isKing() || this.isOwnedBy(player) || this.isDormant() || healing(stack) > 0 ? InteractionResult.SUCCESS : super.mobInteract(player, hand);
		}

		if (!this.isTame()) {
			if (treat && this.isKing()) {
				this.playSound(Mimicry.MIMIC_GROWL, 1.5F, this.getVoicePitch());
				return InteractionResult.FAIL;
			} else if (treat) {
				stack.consume(1, player);
				this.tryToTame(player);
			} else if (this.isDormant()) {
				this.wake(player);
			} else {
				return super.mobInteract(player, hand);
			}
		} else if (!this.isOwnedBy(player)) {
			return super.mobInteract(player, hand);
		} else if (healing(stack) > 0 && this.getHealth() < this.getMaxHealth()) {
			this.usePlayerItem(player, hand, stack);
			this.heal(healing(stack));
			this.playSound(Mimicry.MIMIC_GULP, 1.0F, this.getVoicePitch());
			this.level().broadcastEntityEvent(this, EVENT_CHOMP);
			this.level().broadcastEntityEvent(this, (byte) 7);
		} else if (player.isSecondaryUseActive()) {
			this.setOrderedToSit(!this.isOrderedToSit());
			this.jumping = false;
			this.navigation.stop();
			this.setTarget(null);
		} else {
			player.openMenu(new SimpleMenuProvider((id, playerInventory, p) -> ChestMenu.threeRows(id, playerInventory, this.inventory), this.getDisplayName()));
		}
		return InteractionResult.SUCCESS;
	}

	private float healing(ItemStack stack) {
		if (stack.is(Items.GOLD_BLOCK) || stack.is(Items.RAW_GOLD_BLOCK)) {
			return this.getMaxHealth();
		}
		return stack.is(Mimicry.MIMIC_FOOD) ? 8.0F : stack.is(Items.GOLD_NUGGET) ? 2.0F : 0.0F;
	}

	private void tryToTame(Player player) {
		this.playSound(Mimicry.MIMIC_GULP, 1.0F, this.getVoicePitch());
		if (this.random.nextInt(3) == 0) {
			this.tame(player);
			this.setDormant(false);
			this.navigation.stop();
			this.setTarget(null);
			this.level().broadcastEntityEvent(this, (byte) 7);
			this.playSound(Mimicry.MIMIC_HAPPY, 1.0F, this.getVoicePitch());
		} else {
			this.level().broadcastEntityEvent(this, (byte) 6);
			if (this.isDormant()) {
				this.wake(player);
			}
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (this.isDormant()) {
			this.wake(source.getEntity() instanceof LivingEntity attacker ? attacker : null);
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		if (this.getAttack() == ATTACK_NONE) { // leap attacks animate their own bite
			this.level().broadcastEntityEvent(this, EVENT_CHOMP);
		}
		this.playSound(Mimicry.MIMIC_CHOMP, 1.0F, this.getVoicePitch());
		return super.doHurtTarget(level, target);
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == EVENT_CHOMP) {
			this.chompTicks = 4;
		} else {
			super.handleEntityEvent(id);
		}
	}

	// snap the sitting yaw before anything else this tick can rotate it
	@Override
	public void setInSittingPose(boolean sitting) {
		super.setInSittingPose(sitting);
		if (sitting && Float.isNaN(this.sitYaw) && !this.level().isClientSide()) {
			this.sitYaw = Math.round(this.getYRot() / 90.0F) * 90.0F;
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.isInSittingPose()) {
			if (this.level().isClientSide()) {
				this.sitYaw = this.getYRot(); // already snapped by the server
			} else if (Float.isNaN(this.sitYaw)) {
				this.sitYaw = Math.round(this.getYRot() / 90.0F) * 90.0F;
			}
			this.setYRot(this.sitYaw);
			this.yRotO = this.sitYaw;
			this.setYBodyRot(this.sitYaw);
			this.yBodyRotO = this.sitYaw;
			this.setYHeadRot(this.sitYaw);
			this.yHeadRotO = this.sitYaw;
		} else {
			this.sitYaw = Float.NaN;
		}
		if (this.level().isClientSide()) {
			this.lidO = this.lid;
			this.lid = Mth.clamp(this.lid + (this.isOpenAsChest() ? 0.1F : -0.1F), 0.0F, 1.0F);
			this.mouthO = this.mouth;
			float target;
			if (this.chompTicks > 0) {
				this.chompTicks--;
				target = 1.0F;
			} else if (this.isDormant() || this.isInSittingPose() || this.getAttack() == ATTACK_RECOVER) {
				target = 0.0F;
			} else if (this.getAttack() == ATTACK_CHARGE || this.getAttack() == ATTACK_AIR) {
				target = !this.isKing() ? 1.0F : this.getAttack() == ATTACK_AIR ? 0.0F : 0.05F + 0.04F * Mth.sin(this.tickCount * 1.7F);
			} else if (this.isTame()) {
				target = 0.12F;
			} else {
				target = 0.3F + 0.1F * Mth.sin(this.tickCount * 0.4F);
			}
			this.mouth += (target - this.mouth) * 0.5F;
			this.animateLeap();
		}
	}

	private void animateLeap() {
		this.attackTicks++;
		this.squeezeO = this.squeeze;
		this.squeeze = Mth.approach(this.squeeze, this.isSqueezed() ? 1.0F : 0.0F, 0.2F);
		this.flapO = this.flap;
		this.flapSpeed = (this.flapSpeed + ((this.getAttack() == ATTACK_AIR ? 1.0F : 0.0F) - this.flap) * 0.3F) * 0.7F;
		this.flap = Mth.clamp(this.flap + this.flapSpeed, 0.0F, 1.0F);
		if (this.flap == 0.0F) {
			this.flapSpeed = 0.0F;
		}
		this.flipO = this.flip;
		this.flip = this.somersault();
		this.leanO = this.lean;
		this.lean = Mth.approach(this.lean, this.getAttack() == ATTACK_AIR && !this.isKing() ? 1.0F : 0.0F, 0.34F);
		this.liftO = this.lift;
		this.lift *= 0.85F;
		this.landingO = this.landing;
		this.landing *= 0.8F;
		if (this.getAttack() == ATTACK_CHARGE) {
			boolean king = this.isKing();
			float charge = this.getCharge(0.0F);
			BlockState ground = this.getBlockStateOn();
			double reach = this.getBbWidth() * 0.55;
			for (int i = king ? 0 : 1; i < 2 && !ground.isAir() && (king || this.attackTicks % 2 == 0); i++) {
				double angle = this.random.nextDouble() * Mth.TWO_PI;
				double dx = Math.cos(angle);
				double dz = Math.sin(angle);
				this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), this.getX() + dx * reach, this.getY() + 0.05, this.getZ() + dz * reach,
					dx * 0.15, 0.1 + 0.15 * charge, dz * 0.15);
			}
			if (king && this.random.nextFloat() < charge) {
				this.level().addParticle(ParticleTypes.WAX_ON, this.getRandomX(0.6), this.getY() + this.getBbHeight() * (0.5 + 0.4 * this.random.nextDouble()),
					this.getRandomZ(0.6), 0.0, 0.05, 0.0);
			}
		}
	}

	private float somersault() {
		if (!this.isKing()) {
			return 0.0F;
		}
		if (this.getAttack() == ATTACK_AIR) {
			float t = Math.min(1.0F, this.attackTicks / (float) this.entityData.get(DATA_FLIGHT));
			if (t < 0.2F) {
				return 0.3F * t / 0.2F;
			}
			float over = (t - 0.2F) / 0.8F;
			return Mth.lerp(over * over * (3.0F - 2.0F * over), 0.3F, -2.8F);
		}
		if (this.getAttack() == ATTACK_RECOVER) {
			float u = Mth.clamp((this.attackTicks - 3) / 11.0F, 0.0F, 1.0F) - 1.0F;
			return this.landedFlip * -(1.2F * u * u + 2.2F * u * u * u); // ease-out with a slight overshoot
		}
		return 0.0F;
	}

	public float getFlip(float partialTicks) {
		return Mth.lerp(partialTicks, this.flipO, this.flip);
	}

	public float getCharge(float partialTicks) {
		if (this.getAttack() != ATTACK_CHARGE) {
			return 0.0F;
		}
		float t = Math.min(1.0F, (this.attackTicks + partialTicks) / MimicLeap.chargeTicks(this));
		return 1.0F - (1.0F - t) * (1.0F - t);
	}

	public float getLean(float partialTicks) {
		return Mth.lerp(partialTicks, this.leanO, this.lean);
	}

	public float getSqueeze(float partialTicks) {
		return Mth.lerp(partialTicks, this.squeezeO, this.squeeze);
	}

	public float getFlap(float partialTicks) {
		return Mth.lerp(partialTicks, this.flapO, this.flap);
	}

	public float getLift(float partialTicks) {
		return Mth.lerp(partialTicks, this.liftO, this.lift);
	}

	public float getLanding(float partialTicks) {
		return Mth.lerp(partialTicks, this.landingO, this.landing);
	}

	public float getMouthOpen(float partialTicks) {
		return Mth.lerp(partialTicks, this.mouthO, this.mouth);
	}

	public float getLidOpen(float partialTicks) {
		float t = 1.0F - Mth.lerp(partialTicks, this.lidO, this.lid);
		return 1.0F - t * t * t;
	}

	@Override
	protected void dropEquipment(ServerLevel level) {
		super.dropEquipment(level);
		this.inventory.removeAllItems().forEach(stack -> this.spawnAtLocation(level, stack));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		this.writeInventoryToTag(output);
		output.putBoolean("Dormant", this.isDormant());
		output.putBoolean("King", this.bossEvent != null);
		output.putBoolean("Squeezed", this.isSqueezed());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.readInventoryFromTag(input);
		this.setDormant(input.getBooleanOr("Dormant", false));
		this.setSqueezed(input.getBooleanOr("Squeezed", false));
		if (input.getBooleanOr("King", false)) {
			this.entityData.set(DATA_KING, true);
			this.bossEvent = new ServerBossEvent(this.getUUID(), this.getDisplayName(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
		}
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		if (this.isDormant() || this.isInSittingPose()) {
			return null;
		}
		return this.isTame() ? Mimicry.MIMIC_HAPPY : Mimicry.MIMIC_GROWL;
	}

	@Override
	public int getAmbientSoundInterval() {
		return this.isTame() ? 240 : 120;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return Mimicry.MIMIC_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return Mimicry.MIMIC_DEATH;
	}

	@Override
	protected float nextStep() {
		return this.moveDist + (this.isTame() ? 3.0F : 1.5F);
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState blockState) {
		this.playSound(Mimicry.MIMIC_HOP, 0.4F, this.getVoicePitch());
	}

	@Override
	public boolean isFood(ItemStack itemStack) {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}
}
