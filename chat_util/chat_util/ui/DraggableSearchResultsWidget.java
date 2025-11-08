package me.cutebow.chat_util.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.OrderedText;

import java.util.ArrayList;
import java.util.List;

public class DraggableSearchResultsWidget extends ClickableWidget implements Draggable {
    private boolean editable;
    private boolean dragging;
    private int dx, dy;
    private String configKey;
    private int themeARGB = 0xAA000000;
    private final List<OrderedText> lines = new ArrayList<>();
    private int lineHeight = 9;

    public DraggableSearchResultsWidget(int x, int y, int w, int h) {
        super(x, y, w, h, null);
        this.active = false;
        this.visible = false;
    }

    public void setThemeARGB(int argb) { this.themeARGB = argb; }
    public void setActive(boolean active) { this.active = active; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public void setLines(List<OrderedText> list, TextRenderer tr, int maxWidth, int maxLines) {
        this.lines.clear();
        if (list != null) this.lines.addAll(list);
        int linesToShow = this.lines.isEmpty() ? Math.min(6, Math.max(1, maxLines)) : Math.min(maxLines, this.lines.size());
        int w = 0;
        for (int i = Math.max(0, this.lines.size() - linesToShow); i < this.lines.size(); i++) {
            w = Math.max(w, tr.getWidth(this.lines.get(i)));
        }
        this.lineHeight = tr.fontHeight;
        this.width = Math.min(Math.max(120, w + 8), maxWidth);
        this.height = linesToShow * lineHeight + 6;
    }

    public void setVisiblePanel(boolean v) { this.visible = v; }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!this.visible) return;
        int bg = 0xCC000000;
        ctx.fill(getX(), getY(), getX() + width, getY() + height, bg);
        int y = getY() + 3;
        int start = Math.max(0, lines.size() - Math.max(1, (height - 6) / lineHeight));
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        for (int i = start; i < lines.size(); i++) {
            ctx.drawText(tr, lines.get(i), getX() + 4, y, 0xFFFFFF, true);
            y += lineHeight;
        }
        int c = themeARGB;
        ctx.fill(getX(), getY() - 1, getX() + width, getY(), c);
        ctx.fill(getX(), getY() + height, getX() + width, getY() + height + 1, c);
        ctx.fill(getX() - 1, getY(), getX(), getY() + height, c);
        ctx.fill(getX() + width, getY(), getX() + width + 1, getY() + height, c);
    }

    @Override public boolean isMouseOver(double mx, double my) {
        return this.visible && mx >= getX() && my >= getY() && mx < getX()+width && my < getY()+height;
    }

    @Override public void setEditable(boolean e){ editable=e; if(!e) dragging=false; }
    @Override public boolean isDragging(){ return dragging; }
    @Override public void beginDrag(int mx,int my){ if(editable && isMouseOver(mx,my)){ dragging=true; dx=(int)mx-getX(); dy=(int)my-getY(); } }
    @Override public void endDrag(){ dragging=false; }
    @Override public void dragTo(int x,int y){ setX(x - dx); setY(y - dy); }
    @Override public int getDragDX(){ return dx; }
    @Override public int getDragDY(){ return dy; }
    @Override public int getWidth(){ return width; }
    @Override public int getHeight(){ return height; }
    @Override public String getConfigKey(){ return configKey; }
    @Override public void setConfigKey(String key){ this.configKey = key; }

    @Override public int getX(){ return super.getX(); }
    @Override public int getY(){ return super.getY(); }
    @Override public void setX(int x){ super.setX(x); }
    @Override public void setY(int y){ super.setY(y); }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
}
