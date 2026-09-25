package com.slainlight.mimicry.platform;

//? if >=1.20.5 {
import com.mojang.serialization.Codec;
//?}
import com.slainlight.mimicry.Mimicry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
//? if <26.1 {
/*import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
*///?}
//? if fabric && >=1.20.5 {
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
//?} else if neoforge {
/*import net.neoforged.neoforge.attachment.AttachmentType;
*///?} else {
/*import net.minecraft.world.level.saveddata.SavedData;
*///?}

public final class Platform {
	private Platform() {
	}

	// Bram's quest: 0 = not started, 1 = collecting sigils, 2 = done
	//? if fabric && >=1.20.5 {
	private static final AttachmentType<Integer> BRAM_QUEST = AttachmentRegistry.create(Mimicry.id("bram_quest"),
		builder -> builder.persistent(Codec.INT).copyOnDeath().initializer(() -> 0));

	public static int questStage(Player player) {
		return player.getAttachedOrCreate(BRAM_QUEST);
	}

	public static void setQuestStage(Player player, int stage) {
		player.setAttached(BRAM_QUEST, stage);
	}

	private static final AttachmentType<CompoundTag> KEEP = AttachmentRegistry.create(Mimicry.id("keep"),
		builder -> builder.persistent(CompoundTag.CODEC).copyOnDeath().initializer(() -> new CompoundTag()));

	public static CompoundTag keepData(Player player) {
		return player.getAttachedOrCreate(KEEP);
	}

	// saved along with the player
	public static void markKeepDataDirty(Player player) {
	}
	//?} else if neoforge {
	/*public static final AttachmentType<Integer> BRAM_QUEST = AttachmentType.builder(() -> 0).serialize(/^? if >=26.1 {^/Codec.INT.fieldOf("stage")/^?} else {^//^Codec.INT^//^?}^/).copyOnDeath().build();

	public static int questStage(Player player) {
		return player.getData(BRAM_QUEST);
	}

	public static void setQuestStage(Player player, int stage) {
		player.setData(BRAM_QUEST, stage);
	}

	public static final AttachmentType<CompoundTag> KEEP = AttachmentType.builder(() -> new CompoundTag())
		.serialize(/^? if >=26.1 {^/CompoundTag.CODEC.fieldOf("data")/^?} else {^//^CompoundTag.CODEC^//^?}^/).copyOnDeath().build();

	public static CompoundTag keepData(Player player) {
		return player.getData(KEEP);
	}

	// saved along with the player
	public static void markKeepDataDirty(Player player) {
	}
	*///?} else {
	/*// no player attachments here, so player data is kept per UUID in the overworld's saved data
	private static final class PlayerData extends SavedData {
		private final CompoundTag players;

		private PlayerData(CompoundTag players) {
			this.players = players;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			return tag.merge(this.players);
		}
	}

	private static PlayerData data(Player player, String name) {
		return player.getServer().overworld().getDataStorage().computeIfAbsent(PlayerData::new, () -> new PlayerData(new CompoundTag()), name);
	}

	public static int questStage(Player player) {
		return data(player, "mimicry_quests").players.getInt(player.getStringUUID());
	}

	public static void setQuestStage(Player player, int stage) {
		PlayerData quests = data(player, "mimicry_quests");
		quests.players.putInt(player.getStringUUID(), stage);
		quests.setDirty();
	}

	public static CompoundTag keepData(Player player) {
		CompoundTag players = data(player, "mimicry_keep").players;
		if (!(players.get(player.getStringUUID()) instanceof CompoundTag)) {
			players.put(player.getStringUUID(), new CompoundTag());
		}
		return players.getCompound(player.getStringUUID());
	}

	public static void markKeepDataDirty(Player player) {
		data(player, "mimicry_keep").setDirty();
	}
	*///?}

	//? if <26.1 {
	/*// versions without vanilla dialogs show Bram's dialog files in a screen of our own
	public static void openDialog(ServerPlayer player, Identifier dialog) {
		//? if forge {
		/^com.slainlight.mimicry.forge.ForgeNetwork.openDialog(player, dialog);
		^///?} else if fabric && <1.20.5 {
		/^net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, Mimicry.id("open_dialog"),
			net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create().writeResourceLocation(dialog));
		^///?} else if fabric {
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new OpenDialogPayload(dialog));
		//?} else {
		/^net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new OpenDialogPayload(dialog));
		^///?}
	}
	*///?}
}
