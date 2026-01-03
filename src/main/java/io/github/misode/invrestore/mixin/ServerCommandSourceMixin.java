package io.github.misode.invrestore.mixin;

import io.github.misode.invrestore.util.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(CommandSourceStack.class)
public class ServerCommandSourceMixin {

    @Inject(method = "sendSuccess", at = @At("HEAD"), cancellable = true)
    private void onSendSuccess(Supplier<Component> feedbackSupplier, boolean broadcastToOps, CallbackInfo ci) {
        if (CommandContext.isInvRestoreCommand()) {
            CommandSourceStack self = (CommandSourceStack) (Object) this;

            if (self.getPlayer() != null) {
                ServerPlayer player = self.getPlayer();
                Component feedbackText = feedbackSupplier.get();
                player.sendSystemMessage(feedbackText);
            }

            CommandContext.clear();
            ci.cancel();
        }
    }

    @Inject(method = "sendFailure", at = @At("HEAD"), cancellable = true)
    private void onSendFailure(Component message, CallbackInfo ci) {
        if (CommandContext.isInvRestoreCommand()) {
            CommandSourceStack self = (CommandSourceStack) (Object) this;

            if (self.getPlayer() != null) {
                ServerPlayer player = self.getPlayer();
                player.sendSystemMessage(message);
            }

            CommandContext.clear();
            ci.cancel();
        }
    }
}
