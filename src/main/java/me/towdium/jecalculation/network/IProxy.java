package me.towdium.jecalculation.network;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Author: towdium
 * Date:   17-10-10.
 */
public interface IProxy {
    default void initPre() {
    }

    default void init() {
    }

    default void initPost() {
    }

    default EntityPlayer getPlayer() {
        return null;
    }
}
