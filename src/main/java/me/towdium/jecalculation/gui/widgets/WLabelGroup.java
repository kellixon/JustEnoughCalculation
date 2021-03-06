package me.towdium.jecalculation.gui.widgets;

import mcp.MethodsReturnNonnullByDefault;
import me.towdium.jecalculation.data.label.ILabel;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Author: towdium
 * Date:   17-8-17.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SideOnly(Side.CLIENT)
public class WLabelGroup extends WContainer {
    ArrayList<WLabel> labels = new ArrayList<>();
    ListenerValue<? super WLabelGroup, Integer> listener;

    public WLabelGroup(int xPos, int yPos, int column, int row, WLabel.Mode mode) {
        this(xPos, yPos, column, row, 18, 18, mode);
    }

    public WLabelGroup(int xPos, int yPos, int column, int row, int xSize, int ySize, WLabel.Mode mode) {
        IntStream.range(0, row).forEach(r -> IntStream.range(0, column).forEach(c -> {
            WLabel l = new WLabel(xPos + c * xSize, yPos + r * ySize, xSize, ySize, mode).setListener((i, v) -> {
                if (listener != null) listener.invoke(this, r * column + c);
            });
            labels.add(l);
            add(l);
        }));
    }

    public ILabel get(int index) {
        return labels.get(index).getLabel();
    }

    public List<ILabel> getLabels() {
        return labels.stream().map(WLabel::getLabel).collect(Collectors.toList());
    }

    public void setLabel(ILabel label, int index) {
        labels.get(index).setLabel(label);
    }

    public void setLabel(List<ILabel> labels, int start) {
        for (WLabel label : this.labels) label.setLabel(start < labels.size() ? labels.get(start++) : ILabel.EMPTY);
    }

    public WLabelGroup setListener(ListenerValue<? super WLabelGroup, Integer> listener) {
        this.listener = listener;
        return this;
    }
}
