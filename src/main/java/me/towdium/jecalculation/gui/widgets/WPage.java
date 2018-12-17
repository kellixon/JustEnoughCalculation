package me.towdium.jecalculation.gui.widgets;

import mcp.MethodsReturnNonnullByDefault;
import me.towdium.jecalculation.data.label.ILabel.RegistryEditor.Record;
import me.towdium.jecalculation.gui.JecaGui;
import me.towdium.jecalculation.gui.Resource;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static me.towdium.jecalculation.gui.Resource.*;

/**
 * Author: towdium
 * Date:   17-9-16.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SideOnly(Side.CLIENT)
public class WPage extends WTooltip {
    protected int index;
    protected Record record;
    protected boolean focused;
    protected ListenerAction<? super WPage> listener;

    public WPage(int index, Record record, boolean focused) {
        super(record.localizeKey);
        this.index = index;
        this.record = record;
        this.focused = focused;
    }

    @Override
    public void onDraw(JecaGui gui, int xMouse, int yMouse) {
        Resource resource = focused ? (index == 0 ? WGT_PAGER_F0 : WGT_PAGER_FN) : WGT_PANEL_N;
        gui.drawResourceContinuous(resource, index * 24, -21, 24, 25, 4, 4, 4, 4);
        record.representation.drawLabel(gui, index * 24 + 4, -17, false);
        super.onDraw(gui, xMouse, yMouse);
    }

    @Override
    public boolean onTooltip(JecaGui gui, int xMouse, int yMouse, List<String> tooltip) {
        return super.onTooltip(gui, xMouse, yMouse, tooltip) || mouseIn(xMouse, yMouse);
    }

    @Override
    public boolean onClicked(JecaGui gui, int xMouse, int yMouse, int button) {
        boolean ret = JecaGui.mouseIn(index * 24, -21, 24, 21, xMouse, yMouse) && listener != null && !focused;
        if (ret) listener.invoke(this);
        return ret;
    }

    @Override
    public boolean mouseIn(int xMouse, int yMouse) {
        return JecaGui.mouseIn(index * 24, -21, 24, 21, xMouse, yMouse);
    }

    public WPage setListener(ListenerAction<? super WPage> r) {
        listener = r;
        return this;
    }
}
