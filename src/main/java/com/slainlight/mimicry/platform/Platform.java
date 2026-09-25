package com.slainlight.mimicry.platform;

//? if >=1.20.5 {
import com.mojang.serialization.Codec;
//?}
import com.slainlight.mimicry.Mimicry;
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
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
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
	//?} else if neoforge {
	/*public static final AttachmentType<Integer> BRAM_QUEST = AttachmentType.builder(() -> 0).serialize(/^? if >=26.1 {^/Codec.INT.fieldOf("stage")/^?} else {^//^Codec.INT^//^?}^/).copyOnDeath().build();

	public static int questStage(Player player) {
		return player.getData(BRAM_QUEST);
	}

	public static void setQuestStage(Player player, int stage) {
		player.setData(BRAM_QUEST, stage);
	}
	*///?} else {
	/*// no player attachments here, so stages are kept per player UUID in the overworld's saved data
	private static final class QuestData extends SavedData {
		private final CompoundTag stages;

		private QuestData(CompoundTag stages) {
			this.stages = stages;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			return tag.merge(this.stages);
		}
	}

	private static QuestData quests(Player player) {
		return player.getServer().overworld().getDataStorage().computeIfAbsent(QuestData::new, () -> new QuestData(new CompoundTag()), "mimicry_quests");
	}

	public static int questStage(Player player) {
		return quests(player).stages.getInt(player.getStringUUID());
	}

	public static void setQuestStage(Player player, int stage) {
		QuestData quests = quests(player);
		quests.stages.putInt(player.getStringUUID(), stage);
		quests.setDirty();
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
