package io.github.misode.invrestore.gui;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.gui.SimpleGui;
import eu.pb4.sgui.api.gui.SlotGuiInterface;
import io.github.misode.invrestore.Styles;
import io.github.misode.invrestore.data.Snapshot;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.LinkedHashSet;
import java.util.List;

public class SnapshotGui extends SimpleGui {
    private final Snapshot snapshot;
    private final Container container;
    private final ItemStack viewInventory = Items.CHEST.getDefaultInstance();
    private final ItemStack viewEnderChest = Items.ENDER_CHEST.getDefaultInstance();
    private final ItemStack restoreInventory = Items.OAK_PLANKS.getDefaultInstance();
    private final ItemStack restoreEnderChest = Items.CRYING_OBSIDIAN.getDefaultInstance();
    private final ItemStack restoreEverything = Items.NETHER_STAR.getDefaultInstance();
    private final ItemStack restorePosition = Items.ENDER_PEARL.getDefaultInstance();
    private final ItemStack restoreExperience = Items.EXPERIENCE_BOTTLE.getDefaultInstance();
    private final ItemStack restoreSaturation = Items.GOLDEN_CARROT.getDefaultInstance();
    private final ItemStack restoreFood = Items.COOKED_BEEF.getDefaultInstance();
    private final ItemStack restoreHealth = Items.POTION.getDefaultInstance();

    public SnapshotGui(ServerPlayer player, Snapshot snapshot) {
        super(MenuType.GENERIC_9x6, player, false);
        this.snapshot = snapshot;
        List<ItemStack> items = snapshot.contents().allItems().toList();
        this.container = new TakeOnlyContainer(items);
        this.setTitle(Component.empty()
                .append(snapshot.event().formatEmoji(true))
                .append(" ")
                .append(Component.literal(snapshot.formatTimeAgo()).withStyle(Styles.GUI_DEFAULT))
                .append(" ")
                .append(Component.literal(snapshot.playerName()).withStyle(Styles.GUI_HIGHLIGHT))
        );
        viewInventory.set(DataComponents.ITEM_NAME, Component.literal("View Inventory").withStyle(Styles.LIST_HIGHLIGHT));
        restoreInventory.set(DataComponents.ITEM_NAME, Component.literal("Restore Inventory").withStyle(Styles.LIST_HIGHLIGHT));
        restoreEnderChest.set(DataComponents.ITEM_NAME, Component.literal("Restore Ender Chest").withStyle(Styles.LIST_HIGHLIGHT));
        restoreEverything.set(DataComponents.ITEM_NAME, Component.literal("Restore Everything").withStyle(Styles.LIST_HIGHLIGHT));
        restorePosition.set(DataComponents.ITEM_NAME, Component.literal("Restore Position").withStyle(Styles.LIST_HIGHLIGHT));
        restoreExperience.set(DataComponents.ITEM_NAME, Component.literal("Restore Experience").withStyle(Styles.LIST_HIGHLIGHT));
        restoreSaturation.set(DataComponents.ITEM_NAME, Component.literal("Restore Saturation").withStyle(Styles.LIST_HIGHLIGHT));
        restoreFood.set(DataComponents.ITEM_NAME, Component.literal("Restore Food").withStyle(Styles.LIST_HIGHLIGHT));
        restoreHealth.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.HEALING));
        restoreHealth.set(DataComponents.CUSTOM_NAME, Component.literal("Restore Health").withStyle(Styles.LIST_HIGHLIGHT.withItalic(false)));
        var hiddenComponents = new LinkedHashSet<DataComponentType<?>>();
        hiddenComponents.add(DataComponents.POTION_CONTENTS);
        restoreHealth.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(false, hiddenComponents));
        initDefaultView();
    }

    private void mapSlot(int guiSlot, int containerSlot) {
        this.setSlotRedirect(guiSlot, new SnapshotSlot(this.container, containerSlot, 0, 0));
    }

    private void initDefaultView() {
        for (int i = 0; i < this.size; i++) {
            if (i < 27) {
                this.mapSlot(i, i+9);
            } else if (i < 36) {
                this.mapSlot(i, i-27);
            } else if (i == 45) {
                this.setSlot(i, viewEnderChest, this::handleEnderChestClick);
            } else if (i == 46) {
                this.setSlot(i, restoreInventory, this::handleRestoreInventoryClick);
            } else if (i == 47) {
                this.setSlot(i, restoreEverything, this::handleRestoreEverythingClick);
            } else if (i == 49) {
                this.setSlot(i, restorePosition, this::handleRestorePositionClick);
            } else if (i == 50) {
                this.setSlot(i, restoreExperience, this::handleRestoreExperienceClick);
            } else if (i == 51) {
                this.setSlot(i, restoreSaturation, this::handleRestoreSaturationClick);
            } else if (i == 52) {
                this.setSlot(i, restoreFood, this::handleRestoreFoodClick);
            } else if (i == 53) {
                this.setSlot(i, restoreHealth, this::handleRestoreHealthClick);
            } else if (i == 40) {
                this.mapSlot(i, i);
            } else if (i >= 41) {
                this.mapSlot(i, i-5);
            } else {
                this.setSlot(i, ItemStack.EMPTY);
            }
        }
    }

    private void handleRestoreEverythingClick(int index, ClickType type, Object action, SlotGuiInterface gui) {
        if (type.isLeft && !type.shift) {
            this.restoreInventory();
            this.restoreEnderChest();
            this.snapshot.restoreEverything(player);
        }
    }

    private void handleRestorePositionClick(int index, ClickType type, Object action, SlotGuiInterface gui) {
        if (type.isLeft && !type.shift) {
            this.snapshot.restorePosition(player);
        }
    }

    private void handleRestoreExperienceClick(int index, ClickType type, Object action, SlotGuiInterface gui) {
        if (type.isLeft && !type.shift) {
            this.snapshot.restoreExperience(player);
        }
    }

    private void handleRestoreSaturationClick(int index, ClickType type, Object action, SlotGuiInterface gui) {
        if (type.isLeft && !type.shift) {
            this.snapshot.restoreSaturation(player);
        }
    }

    private void handleRestoreFoodClick(int index, ClickType type, Object action, SlotGuiInterface gui) {
        if (type.isLeft && !type.shift) {
            this.snapshot.restoreFood(player);
        }
    }

    private void handleRestoreHealthClick(int index, ClickType type, Object action, SlotGuiInterface gui) {
        if (type.isLeft && !type.shift) {
            this.snapshot.restoreHealth(player);
        }
    }

    private void handleRestoreInventoryClick(int index, ClickType type, Object action, SlotGuiInterface gui) {
        if (type.isLeft && !type.shift) {
            restoreInventory();
        }
    }

    private void restoreInventory() {
        for (int i = 0; i < this.size; i++) {
            if (i < 41) {
                player.getInventory().setItem(i, container.getItem(i));
            }
        }
    }

    private void initEnderChestView() {
        for (int i = 0; i < this.size; i++) {
            if (i < 27) {
                this.mapSlot(i, i+41);
            } else if (i == 45) {
                this.setSlot(i, viewInventory, this::handleChestClick);
            } else if (i == 46) {
                this.setSlot(i, restoreEnderChest, this::handleRestoreEnderchestClick);
            } else if (i == 47) {
                this.setSlot(i, restoreEverything, this::handleRestoreEverythingClick);
            } else if (i == 49) {
                this.setSlot(i, restorePosition, this::handleRestorePositionClick);
            } else if (i == 50) {
                this.setSlot(i, restoreExperience, this::handleRestoreExperienceClick);
            } else if (i == 51) {
                this.setSlot(i, restoreSaturation, this::handleRestoreSaturationClick);
            } else if (i == 52) {
                this.setSlot(i, restoreFood, this::handleRestoreFoodClick);
            } else if (i == 53) {
                this.setSlot(i, restoreHealth, this::handleRestoreHealthClick);
            } else {
                this.setSlot(i, ItemStack.EMPTY);
            }
        }
    }

    private void handleRestoreEnderchestClick(int index, ClickType type, Object action, SlotGuiInterface gui) {
        if (type.isLeft && !type.shift) {
            restoreEnderChest();
        }
    }

    private void restoreEnderChest() {
        for (int i = 0; i < this.size; i++) {
            if (i < 27) {
                player.getEnderChestInventory().setItem(i, container.getItem(i + 41));
            }
        }
    }

    private void handleEnderChestClick(int index, ClickType type, Object action, SlotGuiInterface gui) {
        if (type.isLeft && !type.shift) {
            this.initEnderChestView();
        }
    }

    private void handleChestClick(int index, ClickType type, Object action, SlotGuiInterface gui) {
        if (type.isLeft && !type.shift) {
            this.initDefaultView();
        }
    }
}