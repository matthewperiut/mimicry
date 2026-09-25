package com.slainlight.mimicry.mixin.client;

//? if <1.21 {
/*import com.slainlight.mimicry.client.MaceSounds;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// adds the downloaded mace sounds as a built-in pack
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@ModifyArg(method = "<init>", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/packs/repository/PackRepository;<init>([Lnet/minecraft/server/packs/repository/RepositorySource;)V"))
	private RepositorySource[] mimicry$addMaceSounds(RepositorySource[] sources) {
		RepositorySource[] out = Arrays.copyOf(sources, sources.length + 1);
		out[sources.length] = MaceSounds.source(Minecraft.getInstance().gameDirectory.toPath());
		return out;
	}
}
*///?}
