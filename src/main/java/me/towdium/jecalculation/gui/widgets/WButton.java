package me.towdium.jecalculation.gui.widgets;

import mcp.MethodsReturnNonnullByDefault;
import me.towdium.jecalculation.gui.JecaGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;

import static me.towdium.jecalculation.gui.Resource.*;

/**
 * Author: towdium
 * Date:   17-8-17.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SideOnly(Side.CLIENT)
public abstract class WButton extends WTooltip {
    protected int xPos, yPos, xSize, ySize;
    protected ListenerAction<? super WButton> listener;
    protected boolean disabled;
    protected int[] keys;

    public WButton(int xPos, int yPos, int xSize, int ySize, @Nullable String name) {
        super(name);
        this.xPos = xPos;
        this.yPos = yPos;
        this.xSize = xSize;
        this.ySize = ySize;
    }

    public WButton setListener(ListenerAction<? super WButton> r) {
        listener = r;
        return this;
    }

    @Override
    public void onDraw(JecaGui gui, int xMouse, int yMouse) {
        super.onDraw(gui, xMouse, yMouse);
        boolean hovered = JecaGui.mouseIn(xPos + 1, yPos + 1, xSize - 2, ySize - 2, xMouse, yMouse);
        if (keys != null) for (int i : keys) if (Keyboard.isKeyDown(i)) hovered = true;
        gui.drawResourceContinuous(disabled ? WGT_BUTTON_D : (hovered ? WGT_BUTTON_F : WGT_BUTTON_N)
                , xPos, yPos, xSize, ySize, 5, 5, 5, 5);
    }

    @Override
    public boolean onClicked(JecaGui gui, int xMouse, int yMouse, int button) {
        if (JecaGui.mouseIn(xPos + 1, yPos + 1, xSize - 2, ySize - 2, xMouse, yMouse) &&
                !disabled && button == 0 && listener != null) {
            trigger();
            return true;
        } else return false;
    }

    private void trigger() {
        listener.invoke(this);
        Minecraft.getMinecraft().getSoundHandler().playSound(
                PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean onKey(JecaGui gui, char ch, int code) {
        // JustEnoughCalculation.logger.info(code);
        if (keys != null) for (int i : keys) {
            if (i == code) {
                trigger();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseIn(int xMouse, int yMouse) {
        return JecaGui.mouseIn(xPos + 1, yPos + 1, xSize - 2, ySize - 2, xMouse, yMouse);
    }

    @SuppressWarnings("UnusedReturnValue")
    public WButton setDisabled(boolean b) {
        disabled = b;
        return this;
    }

    @Override
    protected List<String> getSuffix() {
        return disabled ? Arrays.asList("disabled", "enabled", "") : Arrays.asList("enabled", "");
    }

    public WButton setKeyBind(int... keys) {
        this.keys = keys;
        return this;
    }
}
