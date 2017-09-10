package me.towdium.jecalculation.core.entry.entries;

import me.towdium.jecalculation.core.entry.Entry;
import net.minecraft.item.ItemStack;

/**
 * Author: towdium
 * Date:   8/11/17.
 */
public class EntryItemStack implements Entry {
    int amount;
    ItemStack itemStack;

    public EntryItemStack(ItemStack is) {
        this(is, is.getCount());
    }

    // I will copy it!
    public EntryItemStack(ItemStack is, int amount) {
        itemStack = is.copy();
        this.amount = amount;
    }

    private EntryItemStack(EntryItemStack eis) {
        amount = eis.amount;
        itemStack = eis.itemStack;
    }

    @Override
    public Entry increaseAmount() {
        amount++;
        return this;
    }

    @Override
    public Entry increaseAmountLarge() {
        amount += 10;
        return this;
    }

    @Override
    public Entry decreaseAmount() {
        if (amount <= 1) return Entry.EMPTY;
        else {
            amount--;
            return this;
        }
    }

    @Override
    public Entry decreaseAmountLarge() {
        if (amount <= 10) return Entry.EMPTY;
        else {
            amount -= 10;
            return this;
        }
    }

    @Override
    public ItemStack getRepresentation() {
        return itemStack;
    }

    @Override
    public String getAmountString() {
        return Integer.toString(amount);  // TODO format
    }

    @Override
    public Entry copy() {
        return new EntryItemStack(this);
    }
}