package com.slainlight.mimicry.mixin;

import com.slainlight.mimicry.KeepLoot;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
//? if >=1.20.2 {
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
//?}
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// hides used-up keep containers from the player; see KeepLoot
@Mixin(/*? if >=1.20.2 {*/ServerCommonPacketListenerImpl.class/*?} else {*//*ServerGamePacketListenerImpl.class*//*?}*/)
public abstract class PacketListenerMixin {
	//? if >=26.1 {
	private static final String SEND = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V";
	//?} else {
	/*private static final String SEND = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V";
	*///?}

	@ModifyVariable(method = SEND, at = @At("HEAD"), argsOnly = true)
	private Packet<?> mimicry$hideUsedContainers(Packet<?> packet) {
		return (Object) this instanceof ServerGamePacketListenerImpl game ? KeepLoot.hideFrom(game.player, packet) : packet;
	}

	@Inject(method = SEND, at = @At("TAIL"))
	private void mimicry$hideInChunk(Packet<?> packet,
		/*? if >=26.1 {*/io.netty.channel.ChannelFutureListener/*?} else {*//*net.minecraft.network.PacketSendListener*//*?}*/ listener, CallbackInfo ci) {
		if (packet instanceof ClientboundLevelChunkWithLightPacket chunk && (Object) this instanceof ServerGamePacketListenerImpl game) {
			KeepLoot.hideInChunk(game.player, chunk./*? if >=26.3 {*/x()/*?} else {*//*getX()*//*?}*/, chunk./*? if >=26.3 {*/z()/*?} else {*//*getZ()*//*?}*/);
		}
	}
}
