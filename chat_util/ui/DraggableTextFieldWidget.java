package me.cutebow.chat_util.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class DraggableTextFieldWidget extends TextFieldWidget implements Draggable {
    private boolean editable;
    private boolean dragging;
    private int dx, dy;
    private String configKey;

    public DraggableTextFieldWidget(TextRenderer tr, int x, int y, int w, int h, Text msg) {
        super(tr, x, y, w, h, msg);
    }

    @Override public void setEditable(boolean e){ editable=e; if(!e) dragging=false; }
    @Override public boolean isDragging(){ return dragging; }
    @Override public void beginDrag(int mx,int my){ if(editable){ dragging=true; dx = mx - getX(); dy = my - getY(); } }
    @Override public void endDrag(){ dragging=false; }
    @Override public void dragTo(int x,int y){ setX(x - dx); setY(y - dy); }
    @Override public int getDragDX(){ return dx; }
    @Override public int getDragDY(){ return dy; }
    @Override public String getConfigKey(){ return configKey; }
    @Override public void setConfigKey(String key){ this.configKey = key; }

    @Override public int getX(){ return super.getX(); }
    @Override public int getY(){ return super.getY(); }
    @Override public int getWidth(){ return super.getWidth(); }
    @Override public int getHeight(){ return super.getHeight(); }
    @Override public void setX(int x){ super.setX(x); }
    @Override public void setY(int y){ super.setY(y); }

    @Override public boolean mouseClicked(double mx,double my,int b){
        if (editable && this.clicked(mx,my)) { beginDrag((int)mx,(int)my); return true; }
        return super.mouseClicked(mx,my,b);
    }
    @Override public boolean mouseDragged(double mx,double my,int b,double dx,double dy){
        if (editable && dragging) { dragTo((int)mx,(int)my); return true; }
        return super.mouseDragged(mx,my,b,dx,dy);
    }
    @Override public boolean mouseReleased(double mx,double my,int b){
        if (dragging) { endDrag(); return true; }
        return super.mouseReleased(mx,my,b);
    }
}
