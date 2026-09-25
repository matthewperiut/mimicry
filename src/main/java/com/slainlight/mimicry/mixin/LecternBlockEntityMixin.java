package com.slainlight.mimicry.mixin;

//? if <26.1 {
/*import com.slainlight.mimicry.Hollowmere;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// the lectern only counts writable and written books, so its menu would close on the almanac at once
@Mixin(LecternBlockEntity.class)
public abstract class LecternBlockEntityMixin {
	@Inject(method = "hasBook", at = @At("HEAD"), cancellable = true)
	private void mimicry$holdsAlmanac(CallbackInfoReturnable<Boolean> cir) {
		if (((LecternBlockEntity) (Object) this).getBook().is(Hollowmere.ALMANAC_BOOK)) {
			cir.setReturnValue(true);
		}
	}
}
*///?}
