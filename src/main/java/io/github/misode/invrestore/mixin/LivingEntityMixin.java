package io.github.misode.invrestore.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.misode.invrestore.InvRestore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Unique
    private final LivingEntity livingEntity = (LivingEntity) (Object) this;

    @Inject(method = "drop", at = @At("RETURN"))
    private void drop(ItemStack itemStack, boolean bl, boolean bl2, CallbackInfoReturnable<ItemEntity> cir) {
        if (livingEntity instanceof ServerPlayer serverPlayer && cir.getReturnValue() != null) {
            InvRestore.markEntity(serverPlayer.getUUID(), cir.getReturnValue());
        }
    }

    @WrapOperation(method = "dropExperience", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"))
    private void dropExperience(ServerLevel serverLevel, Vec3 vec3, int i, Operation<Void> original) {
        if (livingEntity instanceof ServerPlayer serverPlayer) {
            while (i > 0) {
                int j = ExperienceOrb.getExperienceValue(i);
                i -= j;
                if (!ExperienceOrb.tryMergeToExisting(serverLevel, vec3, j)) {
                    var experienceOrb = new ExperienceOrb(serverLevel, vec3, Vec3.ZERO, j);
                    serverLevel.addFreshEntity(new ExperienceOrb(serverLevel, vec3, Vec3.ZERO, j));
                    InvRestore.markEntity(serverPlayer.getUUID(), experienceOrb);
                }
            }
        } else {
            original.call(serverLevel, vec3, i);
        }
    }
}
