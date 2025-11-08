package me.cutebow.chat_util.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class DraggableButtonWidget extends ButtonWidget implements Draggable {
    private boolean editable;
    private int themeARGB = 0xFF66CCFF;
    private String configKey;
    private int dragDX, dragDY;
    private boolean dragging;
    private int alpha = 255;
    private boolean presetMode = false;

    public DraggableButtonWidget(int x, int y, int w, int h, Text text, PressAction onPress) {
        super(x, y, w, h, text, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    public void setEditable(boolean b){ this.editable = b; }
    public void setThemeARGB(int argb){ this.themeARGB = argb; }
    public void setConfigKey(String k){ this.configKey = k; }
    public String getConfigKey(){ return configKey; }
    public boolean isDragging(){ return dragging; }
    public int getDragDX(){ return dragDX; }
    public int getDragDY(){ return dragDY; }
    public void setAlpha(int a){ this.alpha = Math.max(0, Math.min(255, a)); }
    public void setActive(boolean v){ this.active = v; }
    public void setVisible(boolean v){ this.visible = v; }
    public void setPresetMode(boolean v){ this.presetMode = v; }

    @Override
    public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!presetMode || alpha >= 255) {
            super.renderWidget(ctx, mouseX, mouseY, delta);
            return;
        }
        int a = this.isHovered() ? Math.min(255, alpha + 20) : alpha;
        int bg = (themeARGB & 0x00FFFFFF) | (a << 24);
        ctx.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
        int fontH = MinecraftClient.getInstance().textRenderer.fontHeight;
        int cx = getX() + getWidth() / 2;
        int cy = getY() + (getHeight() - fontH) / 2;
        int color = this.active ? 0xFFFFFF : 0xA0A0A0;
        ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(), cx, cy, color);
    }

    @Override
    public void onClick(double mx, double my) {
        if (editable) {
            dragging = true;
            dragDX = (int)mx - getX();
            dragDY = (int)my - getY();
        } else {
            super.onClick(mx, my);
        }
    }

    @Override
    public void onRelease(double mx, double my) { dragging = false; }

    public void endDrag(){ dragging = false; }

    public boolean isMouseOver(double mx, double my) {
        return mx >= getX() && my >= getY() && mx < getX()+getWidth() && my < getY()+getHeight();
    }

    public void setSize(int w, int h){ this.setDimensions(w, h); }
    public void setX(int x){ super.setX(x); }
    public void setY(int y){ super.setY(y); }
    public int getWidth(){ return super.getWidth(); }
    public int getHeight(){ return super.getHeight(); }
    public int getX(){ return super.getX(); }
    public int getY(){ return super.getY(); }

    public void beginDrag(int mx, int my){
        if (!editable) return;
        if (isMouseOver(mx, my)) {
            dragging = true;
            dragDX = mx - getX();
            dragDY = my - getY();
        }
    }

    public void dragTo(int mx, int my){
        if (!editable || !dragging) return;
        int nx = mx - dragDX;
        int ny = my - dragDY;
        this.setPosition(nx, ny);
    }
}
