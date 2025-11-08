package me.cutebow.chat_util.ui;

import me.cutebow.chat_util.config.ChatUtilConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class NavArrowsOverlay {
    private final Screen screen;
    private final DraggableButtonWidget up;
    private final DraggableButtonWidget down;
    private final DraggableButtonWidget left;
    private final DraggableButtonWidget right;
    private final List<Draggable> all = new ArrayList<>();
    private boolean editable;

    private boolean leftRepeating;
    private boolean rightRepeating;
    private long leftStartMs, leftLastMs;
    private long rightStartMs, rightLastMs;
    private static final long INITIAL_DELAY_MS = 350;
    private static final long REPEAT_INTERVAL_MS = 70;

    public NavArrowsOverlay(Screen screen,
                            ChatUtilConfig cfg,
                            ButtonWidget.PressAction onUp,
                            ButtonWidget.PressAction onDown,
                            ButtonWidget.PressAction onLeft,
                            ButtonWidget.PressAction onRight) {
        this.screen = screen;

        DraggableButtonWidget u = null, d = null, l = null, r = null;

        if (cfg.enablePrevNextButtons) {
            ChatUtilConfig.Pos upP = cfg.positions.getOrDefault("up", new ChatUtilConfig.Pos(6, 30));
            ChatUtilConfig.Pos downP = cfg.positions.getOrDefault("down", new ChatUtilConfig.Pos(28, 30));
            u = new DraggableButtonWidget(upP.x, upP.y, 20, 18, Text.literal("▲"), onUp);
            d = new DraggableButtonWidget(downP.x, downP.y, 20, 18, Text.literal("▼"), onDown);
            u.setConfigKey("up"); d.setConfigKey("down");
            ((me.cutebow.chat_util.mixin.ScreenAccessor)(Object)screen).chatutil$invokeAddDrawableChild(u);
            ((me.cutebow.chat_util.mixin.ScreenAccessor)(Object)screen).chatutil$invokeAddDrawableChild(d);
            all.add(u); all.add(d);
        }

        if (cfg.enableLeftRightButtons) {
            ChatUtilConfig.Pos leftP = cfg.positions.getOrDefault("left", new ChatUtilConfig.Pos(50, 30));
            ChatUtilConfig.Pos rightP = cfg.positions.getOrDefault("right", new ChatUtilConfig.Pos(72, 30));
            l = new DraggableButtonWidget(leftP.x, leftP.y, 20, 18, Text.literal("◀"), onLeft);
            r = new DraggableButtonWidget(rightP.x, rightP.y, 20, 18, Text.literal("▶"), onRight);
            l.setConfigKey("left"); r.setConfigKey("right");
            ((me.cutebow.chat_util.mixin.ScreenAccessor)(Object)screen).chatutil$invokeAddDrawableChild(l);
            ((me.cutebow.chat_util.mixin.ScreenAccessor)(Object)screen).chatutil$invokeAddDrawableChild(r);
            all.add(l); all.add(r);
        }

        this.up = u;
        this.down = d;
        this.left = l;
        this.right = r;
    }

    public List<Draggable> all() { return all; }

    public void setEditable(boolean editable) {
        this.editable = editable;
        for (Draggable d : all) d.setEditable(editable);
    }

    public void render(DrawContext ctx, int argbOutline) {
        for (Draggable d : all) {
            int x = d.getX(), y = d.getY(), w = d.getWidth(), h = d.getHeight();
            ctx.fill(x - 1, y - 1, x + w + 1, y, argbOutline);
            ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, argbOutline);
            ctx.fill(x - 1, y, x, y + h, argbOutline);
            ctx.fill(x + w, y, x + w + 1, y + h, argbOutline);
        }

        if (!editable) {
            MinecraftClient mc = MinecraftClient.getInstance();
            long win = mc.getWindow().getHandle();
            boolean mouseDown = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            int mx = (int)(mc.mouse.getX() / mc.getWindow().getScaleFactor());
            int my = (int)(mc.mouse.getY() / mc.getWindow().getScaleFactor());
            long now = System.nanoTime() / 1_000_000L;

            if (mouseDown && left != null && left.visible && left.isMouseOver(mx, my)) {
                if (!leftRepeating) {
                    leftRepeating = true;
                    leftStartMs = now;
                    leftLastMs = now;
                } else {
                    if (now - leftStartMs >= INITIAL_DELAY_MS && now - leftLastMs >= REPEAT_INTERVAL_MS) {
                        left.onPress();
                        leftLastMs = now;
                    }
                }
            } else {
                leftRepeating = false;
            }

            if (mouseDown && right != null && right.visible && right.isMouseOver(mx, my)) {
                if (!rightRepeating) {
                    rightRepeating = true;
                    rightStartMs = now;
                    rightLastMs = now;
                } else {
                    if (now - rightStartMs >= INITIAL_DELAY_MS && now - rightLastMs >= REPEAT_INTERVAL_MS) {
                        right.onPress();
                        rightLastMs = now;
                    }
                }
            } else {
                rightRepeating = false;
            }
        }
    }

    public void save(ChatUtilConfig cfg) {
        for (Draggable d : all) {
            String key = d.getConfigKey();
            if (key != null) cfg.positions.put(key, new ChatUtilConfig.Pos(d.getX(), d.getY()));
        }
    }
}
