package com.slainlight.mimicry;

import com.slainlight.mimicry.platform.Platform;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
//? if >=26.3 {
import net.minecraft.util.Prediction;
//?}
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
//? if >=26.1 {
import net.minecraft.world.entity.npc.villager.AbstractVillager;
//?} else {
/*import net.minecraft.world.entity.npc.AbstractVillager;
*///?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.20.5 {
import net.minecraft.world.item.trading.ItemCost;
//?}
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
//? if >=26.1 {
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//?} else {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
*///?}
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlacksmithEntity extends AbstractVillager {
	private static final int SIGILS_NEEDED = 3;
	private static final int KNOCKED_OUT_TICKS = 60 * 20;
	private @Nullable BlockPos anvil;
	private @Nullable BlockPos bed;
	private int knockedOutTicks;
	private int awakeUntil;

	public BlacksmithEntity(EntityType<? extends BlacksmithEntity> type, Level level) {
		super(type, level);
		if (this.getNavigation() instanceof GroundPathNavigation navigation) {
			navigation.setCanOpenDoors(true);
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.MAX_HEALTH, 30.0).add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
		this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 0.6));
		this.goalSelector.addGoal(2, new SleepGoal());
		this.goalSelector.addGoal(3, new OpenDoorGoal(this, true));
		this.goalSelector.addGoal(4, new SmithGoal());
		this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.5));
		this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.4));
		this.goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
		this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
	}

	public void settle(BlockPos anvil, BlockPos bed) {
		this.anvil = anvil;
		this.bed = bed;
		this.setHomeTo(anvil, 12);
		this.setPersistenceRequired();
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!this.isAlive() || this.isTrading() || this.isBaby() || player.getItemInHand(hand).is(Hollowmere.BLACKSMITH_SPAWN_EGG)) {
			return super.mobInteract(player, hand);
		}
		if (player instanceof ServerPlayer serverPlayer) {
			if (this.isKnockedOut()) {
				serverPlayer.sendSystemMessage(Component.translatable("npc.mimicry.bram.out_cold").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
				return InteractionResult.SUCCESS;
			}
			if (this.isSleeping()) {
				this.stopSleeping();
			}
			this.awakeUntil = this.tickCount + 30 * 20;
			this.getLookControl().setLookAt(player);
			if (quest(serverPlayer) == 1 && serverPlayer.getInventory()./*? if >=1.21.5 {*/getNonEquipmentItems()/*?} else {*//*items*//*?}*/.stream()
				.noneMatch(stack -> stack.is(Items.FILLED_MAP) && isKeepMap(stack))) {
				// also clears blank keep maps left over from older versions
				NonNullList<ItemStack> items = serverPlayer.getInventory()./*? if >=1.21.5 {*/getNonEquipmentItems()/*?} else {*//*items*//*?}*/;
				items.replaceAll(stack -> isKeepMap(stack) ? ItemStack.EMPTY : stack);
				this.give(player, Hollowmere.roll((ServerLevel) this.level(), Hollowmere.KEEP_MAP, this.position(), player));
				this.say(player, "npc.mimicry.bram.new_map");
			}
			String dialog = switch (quest(serverPlayer)) {
				case 0 -> "bram/intro";
				case 1 -> countSigils(serverPlayer) >= SIGILS_NEEDED ? "bram/sigils" : "bram/waiting";
				default -> "bram/friend";
			};
			//? if >=26.1 {
			serverPlayer.registryAccess().lookupOrThrow(Registries.DIALOG).get(ResourceKey.create(Registries.DIALOG, Mimicry.id(dialog)))
				.ifPresent(serverPlayer::openDialog);
			//?} else {
			/*Platform.openDialog(serverPlayer, Mimicry.id(dialog));
			*///?}
		}
		return InteractionResult.SUCCESS;
	}

	// a sleeping mob's hitbox is too small to click and the bed only reports it's occupied, so bed clicks are forwarded
	public static boolean wakeInBed(Player player, Level level, InteractionHand hand, BlockPos pos) {
		var state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof BedBlock)) {
			return false;
		}
		BlockPos head = state.getValue(BedBlock.PART) == BedPart.HEAD ? pos : pos.relative(state.getValue(BedBlock.FACING));
		List<BlacksmithEntity> sleepers = level.getEntitiesOfClass(BlacksmithEntity.class, new net.minecraft.world.phys.AABB(head).inflate(2.0),
			smith -> smith.getSleepingPos().filter(head::equals).isPresent());
		if (sleepers.isEmpty()) {
			return false;
		}
		sleepers.get(0).mobInteract(player, hand);
		return true;
	}

	public static int handleDialogAction(ServerPlayer player, String action) {
		List<BlacksmithEntity> smiths = player.level().getEntitiesOfClass(BlacksmithEntity.class, player.getBoundingBox().inflate(8.0),
			smith -> smith.isAlive() && !smith.isKnockedOut());
		BlacksmithEntity smith = smiths.stream().min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player))).orElse(null);
		if (smith == null) {
			return 0;
		}
		ServerLevel level = (ServerLevel) player.level();
		switch (action) {
			case "accept" -> {
				if (quest(player) == 0) {
					Platform.setQuestStage(player, 1);
					smith.give(player, Hollowmere.roll(level, Hollowmere.KEEP_MAP, smith.position(), player));
					smith.give(player, Hollowmere.roll(level, Hollowmere.ALMANAC, smith.position(), player));
					smith.say(player, "npc.mimicry.bram.accepted");
				}
			}
			case "turnin" -> {
				if (quest(player) == 1 && countSigils(player) >= SIGILS_NEEDED) {
					player.getInventory().clearOrCountMatchingItems(stack -> stack.is(Hollowmere.KNIGHT_SIGIL), /*? if >=26.3 {*/false, /*?}*/SIGILS_NEEDED, player.inventoryMenu.getCraftSlots());
					Platform.setQuestStage(player, 2);
					smith.give(player, Hollowmere.roll(level, Hollowmere.BRAM_REWARD, smith.position(), player));
					player.giveExperiencePoints(60);
					Optional.ofNullable(level.getServer().getAdvancements()./*? if >=1.20.2 {*/get/*?} else {*//*getAdvancement*//*?}*/(Mimicry.id("bram_quest")))
						.ifPresent(advancement -> player.getAdvancements().award(advancement, "done"));
					smith.playSound(SoundEvents.VILLAGER_CELEBRATE, 1.0F, 0.8F);
					level.sendParticles(ParticleTypes.HAPPY_VILLAGER, smith.getX(), smith.getY() + 1.8, smith.getZ(), 12, 0.4, 0.4, 0.4, 0.0);
					smith.say(player, "npc.mimicry.bram.thanks");
				}
			}
			case "trade" -> {
				if (!smith.isTrading() && !smith.getOffers().isEmpty()) {
					smith.setTradingPlayer(player);
					smith.openTradingScreen(player, smith.getDisplayName(), 1);
				}
			}
			default -> {
				return 0;
			}
		}
		return 1;
	}

	private static int quest(ServerPlayer player) {
		return Platform.questStage(player);
	}

	private static int countSigils(Player player) {
		return player.getInventory().countItem(Hollowmere.KNIGHT_SIGIL);
	}

	private static boolean isKeepMap(ItemStack stack) {
		//? if >=1.20.5 {
		return stack.get(DataComponents.ITEM_NAME) instanceof Component name && name.getContents() instanceof TranslatableContents contents
		//?} else {
		/*return stack.getHoverName().getContents() instanceof TranslatableContents contents
		*///?}
			&& contents.getKey().equals("item.mimicry.keep_map");
	}

	private void give(Player player, List<ItemStack> stacks) {
		stacks.forEach(stack -> player.getInventory().placeItemBackInInventory(stack/*? if >=26.3 {*/, Prediction.SERVER_ONLY/*?}*/));
	}

	private void say(Player player, String key) {
		player.sendSystemMessage(Component.translatable("npc.mimicry.bram.says", Component.translatable(key)).withStyle(ChatFormatting.GOLD));
	}

	@Override
	protected void updateTrades(/*? if >=26.1 {*/ServerLevel level/*?}*/) {
		MerchantOffers offers = this.getOffers();
		//? if >=1.20.5 {
		offers.add(new MerchantOffer(new ItemCost(Items.IRON_INGOT, 6), new ItemStack(Items.EMERALD), 16, 2, 0.05F));
		offers.add(new MerchantOffer(new ItemCost(Mimicry.MIMIC_TOOTH, 4), new ItemStack(Items.EMERALD), 16, 2, 0.05F));
		offers.add(new MerchantOffer(new ItemCost(Hollowmere.KNIGHT_SIGIL, 1), new ItemStack(Items.EMERALD, 3), 12, 5, 0.05F));
		offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 2), new ItemStack(Hollowmere.SUNSTONE_LANTERN, 2), 12, 3, 0.05F));
		offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 6), new ItemStack(Mimicry.TREASURE_LENS), 3, 10, 0.05F));
		offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 4), Optional.of(new ItemCost(Mimicry.MIMIC_TOOTH, 2)), new ItemStack(Mimicry.MIMIC_CHEST), 4, 8, 0.05F));
		offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 3), new ItemStack(Items.SHIELD), 8, 5, 0.05F));
		//?} else {
		/*offers.add(new MerchantOffer(new ItemStack(Items.IRON_INGOT, 6), new ItemStack(Items.EMERALD), 16, 2, 0.05F));
		offers.add(new MerchantOffer(new ItemStack(Mimicry.MIMIC_TOOTH, 4), new ItemStack(Items.EMERALD), 16, 2, 0.05F));
		offers.add(new MerchantOffer(new ItemStack(Hollowmere.KNIGHT_SIGIL, 1), new ItemStack(Items.EMERALD, 3), 12, 5, 0.05F));
		offers.add(new MerchantOffer(new ItemStack(Items.EMERALD, 2), new ItemStack(Hollowmere.SUNSTONE_LANTERN, 2), 12, 3, 0.05F));
		offers.add(new MerchantOffer(new ItemStack(Items.EMERALD, 6), new ItemStack(Mimicry.TREASURE_LENS), 3, 10, 0.05F));
		offers.add(new MerchantOffer(new ItemStack(Items.EMERALD, 4), new ItemStack(Mimicry.MIMIC_TOOTH, 2), new ItemStack(Mimicry.MIMIC_CHEST), 4, 8, 0.05F));
		offers.add(new MerchantOffer(new ItemStack(Items.EMERALD, 3), new ItemStack(Items.SHIELD), 8, 5, 0.05F));
		*///?}
	}

	@Override
	protected void rewardTradeXp(MerchantOffer offer) {
		if (offer.shouldRewardExp()) {
			this.level().addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5, this.getZ(), 3 + this.random.nextInt(4)));
		}
	}

	@Override
	public boolean showProgressBar() {
		return false;
	}

	@Override
	public boolean canBeSeenAsEnemy() {
		return false;
	}

	public boolean isKnockedOut() {
		return this.knockedOutTicks > 0;
	}

	// takes lethal damage as a knockdown instead of dying; /kill and the void still work
	@Override
	public void die(DamageSource source) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !(this.level() instanceof ServerLevel)) {
			super.die(source);
			return;
		}
		this.setHealth(1.0F);
		if (this.isSleeping()) {
			this.stopSleeping();
		}
		this.stopTrading();
		this.getNavigation().stop();
		this.knockedOutTicks = KNOCKED_OUT_TICKS;
		this.setPose(Pose.SLEEPING);
		this.playSound(SoundEvents.PLAYER_BIG_FALL, 1.0F, 0.8F);
	}

	//? if >=1.21.2 {
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return (!this.isKnockedOut() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) && super.hurtServer(level, source, damage);
	}
	//?} else {
	/*@Override
	public boolean hurt(DamageSource source, float damage) {
		return (!this.isKnockedOut() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) && super.hurt(source, damage);
	}
	*///?}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide() && this.knockedOutTicks > 0 && --this.knockedOutTicks == 0) {
			this.setPose(Pose.STANDING);
			this.setHealth(this.getMaxHealth());
			this.playSound(SoundEvents.ARMOR_EQUIP_LEATHER/*? if >=1.20.5 {*/.value()/*?}*/, 1.0F, 0.9F);
		}
	}

	@Override
	protected boolean isImmobile() {
		return super.isImmobile() || this.isSleeping() || this.isKnockedOut();
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.isSleeping() && !this.level().isClientSide() && this.level().isBrightOutside()) {
			this.stopSleeping();
		}
	}

	//? if >=26.1 {
	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (this.anvil != null) {
			output.store("Anvil", BlockPos.CODEC, this.anvil);
		}
		if (this.bed != null) {
			output.store("Bed", BlockPos.CODEC, this.bed);
		}
		output.putInt("KnockedOut", this.knockedOutTicks);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.anvil = input.read("Anvil", BlockPos.CODEC).orElse(null);
		this.bed = input.read("Bed", BlockPos.CODEC).orElse(null);
		this.knockedOutTicks = input.getIntOr("KnockedOut", 0);
		if (this.knockedOutTicks > 0) {
			this.setPose(Pose.SLEEPING);
		}
	}
	//?} else {
	/*@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (this.anvil != null) {
			tag.put("Anvil", NbtUtils.writeBlockPos(this.anvil));
		}
		if (this.bed != null) {
			tag.put("Bed", NbtUtils.writeBlockPos(this.bed));
		}
		tag.putInt("KnockedOut", this.knockedOutTicks);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		//? if >=1.20.5 {
		this.anvil = NbtUtils.readBlockPos(tag, "Anvil").orElse(null);
		this.bed = NbtUtils.readBlockPos(tag, "Bed").orElse(null);
		//?} else {
		/^this.anvil = tag.contains("Anvil") ? NbtUtils.readBlockPos(tag.getCompound("Anvil")) : null;
		this.bed = tag.contains("Bed") ? NbtUtils.readBlockPos(tag.getCompound("Bed")) : null;
		^///?}
		this.knockedOutTicks = tag.getInt("KnockedOut");
		if (this.knockedOutTicks > 0) {
			this.setPose(Pose.SLEEPING);
		}
	}
	*///?}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	private class SmithGoal extends Goal {
		private int blows;
		private int cooldown;

		SmithGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			BlacksmithEntity smith = BlacksmithEntity.this;
			return smith.anvil != null && !smith.isTrading() && smith.level().isBrightOutside() && smith.random.nextInt(400) == 0
				&& Hollowmere.isNear(smith, smith.anvil, 24.0);
		}

		@Override
		public boolean canContinueToUse() {
			return this.blows > 0 && !BlacksmithEntity.this.isTrading() && BlacksmithEntity.this.level().isBrightOutside();
		}

		@Override
		public void start() {
			this.blows = 6 + BlacksmithEntity.this.random.nextInt(6);
			this.cooldown = 0;
		}

		@Override
		public void stop() {
			BlacksmithEntity.this.getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			BlacksmithEntity smith = BlacksmithEntity.this;
			Vec3 target = Vec3.atBottomCenterOf(smith.anvil);
			smith.getLookControl().setLookAt(target.x, target.y + 0.9, target.z);
			if (smith.position().distanceToSqr(target) > 2.6 * 2.6) {
				if (smith.getNavigation().isDone()) {
					smith.getNavigation().moveTo(target.x, target.y, target.z, 0.5);
				}
				return;
			}
			smith.getNavigation().stop();
			if (--this.cooldown <= 0) {
				this.cooldown = 24;
				this.blows--;
				smith./*? if >=26.3 {*/swingForAttack/*?} else {*//*swing*//*?}*/(InteractionHand.MAIN_HAND);
				ServerLevel level = (ServerLevel) smith.level();
				level.playSound(null, smith.anvil, SoundEvents.ANVIL_USE, smith.getSoundSource(), 0.125F, 1.1F + smith.random.nextFloat() * 0.3F);
				level.sendParticles(ParticleTypes.SMALL_FLAME, target.x, target.y + 1.05, target.z, 3, 0.15, 0.02, 0.15, 0.02);
				level.sendParticles(ParticleTypes.CRIT, target.x, target.y + 1.05, target.z, 6, 0.1, 0.05, 0.1, 0.15);
			}
		}
	}

	private class SleepGoal extends Goal {
		SleepGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			BlacksmithEntity smith = BlacksmithEntity.this;
			return smith.bed != null && !smith.isSleeping() && smith.level().isDarkOutside() && !smith.isTrading() && smith.tickCount >= smith.awakeUntil
				&& smith.level().getBlockState(smith.bed).is(BlockTags.BEDS);
		}

		@Override
		public void tick() {
			BlacksmithEntity smith = BlacksmithEntity.this;
			if (Hollowmere.isNear(smith, smith.bed, 1.8)) {
				smith.getNavigation().stop();
				smith.startSleeping(smith.bed);
			} else if (smith.getNavigation().isDone()) {
				smith.getNavigation().moveTo(smith.bed.getX() + 0.5, smith.bed.getY(), smith.bed.getZ() + 0.5, 0.5);
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
