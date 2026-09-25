package com.slainlight.mimicry.platform;

//? if >=1.20.5 && <26.1 {
/*import com.slainlight.mimicry.Mimicry;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// tells the client to show one of Bram's dialogs, on versions without vanilla dialogs
public record OpenDialogPayload(Identifier dialog) implements CustomPacketPayload {
	public static final Type<OpenDialogPayload> TYPE = new Type<>(Mimicry.id("open_dialog"));
	public static final StreamCodec<ByteBuf, OpenDialogPayload> CODEC = Identifier.STREAM_CODEC.map(OpenDialogPayload::new, OpenDialogPayload::dialog);

	@Override
	public Type<OpenDialogPayload> type() {
		return TYPE;
	}
}
*///?}
