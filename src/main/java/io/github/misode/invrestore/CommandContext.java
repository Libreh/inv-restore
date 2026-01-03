package io.github.misode.invrestore.util;

/**
 * Thread-local context for tracking InvRestore command execution.
 * This allows the ServerCommandSourceMixin to identify InvRestore messages
 * without fragile string matching.
 */
public class CommandContext {
    private static final ThreadLocal<Boolean> INV_RESTORE_COMMAND = ThreadLocal.withInitial(() -> false);

    /**
     * Mark the current thread as executing an InvRestore command.
     * This should be called immediately before sending feedback.
     */
    public static void markAsInvRestore() {
        INV_RESTORE_COMMAND.set(true);
    }

    /**
     * Check if the current thread is executing an InvRestore command.
     * @return true if this is an InvRestore command context
     */
    public static boolean isInvRestoreCommand() {
        return INV_RESTORE_COMMAND.get();
    }

    /**
     * Clear the InvRestore command flag.
     * This should be called after message handling to prevent flag leakage.
     */
    public static void clear() {
        INV_RESTORE_COMMAND.set(false);
    }
}
