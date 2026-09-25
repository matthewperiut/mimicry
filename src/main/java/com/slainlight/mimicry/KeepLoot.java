package com.slainlight.mimicry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.slainlight.mimicry.platform.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
//? if >=1.20.5 {
import net.minecraft.resources.ResourceKey;
//?} else {
/*import net.minecraft.resources.Identifier;
*///?}
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
//? if >=26.1 {
import net.minecraft.world.entity.ContainerUser;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import org.jspecify.annotations.Nullable;

// Sunken Keep chests and barrels have per-player loot. Springing or breaking one only removes it for that player: the
// block stays in the world, and PacketListenerMixin and KeepContainerMixin hide it from them.
public final class KeepLoot {
	private static final Set</*? if >=1.20.5 {*/ResourceKey<LootTable>/*?} else {*//*Identifier*//*?}*/> TABLES =
		Set.of(Hollowmere.KEEP_CHEST, Hollowmere.KEEP_CHEST_PRIMED, Hollowmere.KEEP_ARMORY, Hollowmere.KEEP_HOARD);
	private static final Codec<List<ItemStack>> ITEMS = ItemStack./*? if >=1.20.5 {*/OPTIONAL_CODEC/*?} else {*//*CODEC*//*?}*/.listOf();

	private KeepLoot() {
	}

	// the real container is never opened, so it keeps its loot table
	public static boolean isKeepContainer(@Nullable BlockEntity blockEntity) {
		if (!(blockEntity instanceof RandomizableContainerBlockEntity container)) {
			return false;
		}
		var lootTable = Mimicry.lootTable(container);
		return lootTable != null && TABLES.contains(lootTable);
	}

	public static boolean use(ServerLevel level, BlockPos pos, ServerPlayer player) {
		if (!(level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity container) || !isKeepContainer(container)) {
			return false;
		}
		BlockState state = container.getBlockState();
		if (isGone(player, level, pos)) {
			hide(player, pos, state);
		} else if (Mimicry.isMimic(level, pos, state)) {
			spring(level, pos, state, player, container);
		} else {
			Copy copy = copy(level, pos, player, container);
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, copy), container.getDisplayName()));
		}
		return true;
	}

	// breaks only the player's copy; the break itself is cancelled
	public static boolean breakCopy(ServerLevel level, BlockPos pos, ServerPlayer player) {
		if (!(level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity container) || !isKeepContainer(container)) {
			return false;
		}
		BlockState state = container.getBlockState();
		if (isGone(player, level, pos)) {
			return true;
		}
		if (Mimicry.isMimic(level, pos, state)) {
			spring(level, pos, state, player, container);
			return true;
		}
		if (!player.getAbilities().instabuild) {
			Containers.dropContents(level, pos, copy(level, pos, player, container));
			Block.dropResources(state, level, pos, container, player, player.getMainHandItem());
		}
		setGone(player, level, pos);
		return true;
	}

	private static void spring(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player, RandomizableContainerBlockEntity container) {
		MimicEntity mimic = Mimicry.burst(level, pos, state, copy(level, pos, player, container));
		if (mimic != null) {
			setGone(player, level, pos);
			hide(player, pos, state);
			level.addFreshEntity(mimic);
			mimic.wake(player);
		}
	}

	public static boolean isGone(Player player, Level level, BlockPos pos) {
		CompoundTag entry = entry(player, level, pos, false);
		return entry != null && entry.contains("Gone");
	}

	// opened, sprung or broken by this player
	public static boolean isUsed(Player player, Level level, BlockPos pos) {
		return entry(player, level, pos, false) != null;
	}

	private static void setGone(ServerPlayer player, ServerLevel level, BlockPos pos) {
		CompoundTag entry = entry(player, level, pos, true);
		entry.remove("Items");
		entry.putBoolean("Gone", true);
		Platform.markKeepDataDirty(player);
	}

	// per player: {"<dimension>": {"<packed pos>": {Items: [...]} or {Gone: true}}}
	private static @Nullable CompoundTag entry(Player player, Level level, BlockPos pos, boolean create) {
		return child(child(Platform.keepData(player), dimension(level), create), Long.toString(pos.asLong()), create);
	}

	private static @Nullable CompoundTag child(@Nullable CompoundTag parent, String key, boolean create) {
		if (parent == null) {
			return null;
		}
		if (parent.get(key) instanceof CompoundTag existing) {
			return existing;
		}
		if (!create) {
			return null;
		}
		CompoundTag child = new CompoundTag();
		parent.put(key, child);
		return child;
	}

	private static String dimension(Level level) {
		return level.dimension()./*? if >=26.1 {*/identifier()/*?} else {*//*location()*//*?}*/.toString();
	}

	private static boolean isContainer(BlockState state) {
		return state.is(Blocks.CHEST) || state.is(Blocks.BARREL);
	}

	// air, or water if it was waterlogged
	private static void hide(ServerPlayer player, BlockPos pos, BlockState state) {
		player.connection.send(new ClientboundBlockUpdatePacket(pos, state.getFluidState().createLegacyBlock()));
	}

	public static Packet<?> hideFrom(ServerPlayer player, Packet<?> packet) {
		if (packet instanceof ClientboundBlockUpdatePacket update && isContainer(update.getBlockState()) && isGone(player, player.level(), update.getPos())) {
			return new ClientboundBlockUpdatePacket(update.getPos(), update.getBlockState().getFluidState().createLegacyBlock());
		}
		return packet;
	}

	// chunk packets include every container, so used-up ones are hidden again after one is sent
	public static void hideInChunk(ServerPlayer player, int chunkX, int chunkZ) {
		CompoundTag chests = child(Platform.keepData(player), dimension(player.level()), false);
		if (chests == null) {
			return;
		}
		for (String key : chests./*? if >=1.21.5 {*/keySet/*?} else {*//*getAllKeys*//*?}*/()) {
			BlockPos pos = BlockPos.of(Long.parseLong(key));
			if (SectionPos.blockToSectionCoord(pos.getX()) == chunkX && SectionPos.blockToSectionCoord(pos.getZ()) == chunkZ
				&& chests.get(key) instanceof CompoundTag entry && entry.contains("Gone")) {
				BlockState state = player.level().getBlockState(pos);
				if (isContainer(state)) {
					hide(player, pos, state);
				}
			}
		}
	}

	public static boolean passesThrough(BlockPos pos, CollisionContext context) {
		return context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof ServerPlayer player && isGone(player, player.level(), pos);
	}

	// the player's copy, rolled from the container's loot table the first time
	private static Copy copy(ServerLevel level, BlockPos pos, ServerPlayer player, RandomizableContainerBlockEntity container) {
		Copy copy = new Copy(level, pos, player);
		CompoundTag entry = entry(player, level, pos, false);
		Tag saved = entry != null ? entry.get("Items") : null;
		if (saved != null) {
			List<ItemStack> items = ITEMS.parse(ops(level), saved).result().orElse(List.of());
			for (int i = 0; i < Math.min(items.size(), copy.getContainerSize()); i++) {
				copy.setItem(i, items.get(i));
			}
			copy.ready = true;
		} else {
			LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
				.withParameter(LootContextParams.THIS_ENTITY, player).withLuck(player.getLuck()).create(LootContextParamSets.CHEST);
			Hollowmere.lootTable(level, Mimicry.lootTable(container)).fill(copy, params, level.getRandom().nextLong());
			copy.ready = true;
			copy.save();
		}
		return copy;
	}

	private static DynamicOps<Tag> ops(ServerLevel level) {
		return /*? if >=1.20.5 {*/level.registryAccess().createSerializationContext(NbtOps.INSTANCE)/*?} else {*//*NbtOps.INSTANCE*//*?}*/;
	}

	private static final class Copy extends SimpleContainer {
		private final ServerLevel level;
		private final BlockPos pos;
		private final ServerPlayer player;
		private boolean ready;

		Copy(ServerLevel level, BlockPos pos, ServerPlayer player) {
			super(27);
			this.level = level;
			this.pos = pos;
			this.player = player;
		}

		@Override
		public void setChanged() {
			super.setChanged();
			if (this.ready) {
				this.save();
			}
		}

		private void save() {
			List<ItemStack> items = new ArrayList<>();
			for (int i = 0; i < this.getContainerSize(); i++) {
				items.add(this.getItem(i));
			}
			ITEMS.encodeStart(ops(this.level), items).result().ifPresent(tag -> entry(this.player, this.level, this.pos, true).put("Items", tag));
			Platform.markKeepDataDirty(this.player);
		}

		@Override
		public boolean stillValid(Player player) {
			return player.distanceToSqr(Vec3.atCenterOf(this.pos)) <= 64.0;
		}

		@Override
		public void startOpen(/*? if >=26.1 {*/ContainerUser/*?} else {*//*Player*//*?}*/ user) {
			this.lid(true);
		}

		@Override
		public void stopOpen(/*? if >=26.1 {*/ContainerUser/*?} else {*//*Player*//*?}*/ user) {
			this.lid(false);
		}

		// only the player looking in sees it open; everyone nearby hears it
		private void lid(boolean open) {
			BlockState state = this.level.getBlockState(this.pos);
			SoundEvent sound;
			if (state.is(Blocks.BARREL)) {
				this.player.connection.send(new ClientboundBlockUpdatePacket(this.pos, state.setValue(BarrelBlock.OPEN, open)));
				sound = open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE;
			} else {
				this.player.connection.send(new ClientboundBlockEventPacket(this.pos, state.getBlock(), 1, open ? 1 : 0));
				sound = open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE;
			}
			this.level.playSound(null, this.pos, sound, SoundSource.BLOCKS, 0.5F, this.level.getRandom().nextFloat() * 0.1F + 0.9F);
		}
	}
}
