package com.slainlight.mimicry.forge;

//? if forge {
/*import com.mojang.serialization.Codec;
import com.slainlight.mimicry.Hollowmere;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraftforge.common.loot.IGlobalLootModifier;

public record AlmanacLootModifier() implements IGlobalLootModifier {
	public static final Codec<AlmanacLootModifier> CODEC = Codec.unit(AlmanacLootModifier::new);

	@Override
	public ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> loot, LootContext context) {
		Hollowmere.modifyDrops(Optional.ofNullable(context.getQueriedLootTableId()), context, loot);
		return loot;
	}

	@Override
	public Codec<AlmanacLootModifier> codec() {
		return CODEC;
	}
}
*///?}
