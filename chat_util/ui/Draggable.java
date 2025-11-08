package me.cutebow.chat_util.ui;

public interface Draggable {
    void setEditable(boolean editable);
    boolean isDragging();
    void beginDrag(int mouseX, int mouseY);
    void endDrag();
    void dragTo(int x, int y);
    int getDragDX();
    int getDragDY();
    int getX();
    int getY();
    int getWidth();
    int getHeight();
    void setX(int x);
    void setY(int y);
    String getConfigKey();
    void setConfigKey(String key);
}
