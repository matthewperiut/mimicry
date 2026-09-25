package com.slainlight.mimicry.forge;

//? if forge {
/*import com.slainlight.mimicry.Mimicry;
import com.slainlight.mimicry.client.MimicryClient;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ForgeNetwork {
	private static final String VERSION = "1";
	private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(Mimicry.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

	private ForgeNetwork() {
	}

	private record OpenDialog(Identifier dialog) {
	}

	static void init() {
		CHANNEL.messageBuilder(OpenDialog.class, 0, NetworkDirection.PLAY_TO_CLIENT)
			.encoder((message, buf) -> buf.writeResourceLocation(message.dialog()))
			.decoder(buf -> new OpenDialog(buf.readResourceLocation()))
			.consumerMainThread((message, context) -> MimicryClient.openDialog(message.dialog()))
			.add();
	}

	public static void openDialog(ServerPlayer player, Identifier dialog) {
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenDialog(dialog));
	}
}
*///?}
