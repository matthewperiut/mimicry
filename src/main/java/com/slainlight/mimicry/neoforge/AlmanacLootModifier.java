package com.slainlight.mimicry.neoforge;

//? if neoforge {
/*import com.mojang.serialization.MapCodec;
import com.slainlight.mimicry.Hollowmere;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;

public record AlmanacLootModifier() implements IGlobalLootModifier {
	public static final MapCodec<AlmanacLootModifier> CODEC = MapCodec.unit(AlmanacLootModifier::new);

	@Override
	public ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> loot, LootContext context) {
		Hollowmere.modifyDrops(Optional.ofNullable(context.getQueriedLootTableId()).map(id -> ResourceKey.create(Registries.LOOT_TABLE, id)), context, loot);
		return loot;
	}

	@Override
	public MapCodec<AlmanacLootModifier> codec() {
		return CODEC;
	}

	//? if >=26.1 {
	@Override
	public int priority() {
		return DEFAULT_PRIORITY;
	}
	//?}
}
*///?}
