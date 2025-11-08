package me.cutebow.chat_util.mixin;

import me.cutebow.chat_util.ChatUtilClient;
import me.cutebow.chat_util.config.ChatUtilConfig;
import me.cutebow.chat_util.ui.Draggable;
import me.cutebow.chat_util.ui.DraggableButtonWidget;
import me.cutebow.chat_util.ui.DraggableSearchResultsWidget;
import me.cutebow.chat_util.ui.DraggableTextFieldWidget;
import me.cutebow.chat_util.ui.PresetButtonsOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    protected ChatScreenMixin(Text title) { super(title); }

    @Unique private DraggableTextFieldWidget search;
    @Unique private DraggableSearchResultsWidget searchPanel;
    @Unique private DraggableButtonWidget editToggle;
    @Unique private DraggableButtonWidget up;
    @Unique private DraggableButtonWidget down;
    @Unique private DraggableButtonWidget left;
    @Unique private DraggableButtonWidget right;
    @Unique private PresetButtonsOverlay presetsOverlay;
    @Unique private final List<Draggable> draggables = new ArrayList<>();
    @Unique private boolean editMode;
    @Unique private int theme;
    @Unique private boolean leftDown;

    @Unique private boolean leftRepeating;
    @Unique private boolean rightRepeating;
    @Unique private long leftStartMs, leftLastMs;
    @Unique private long rightStartMs, rightLastMs;
    @Unique private static final long INITIAL_DELAY_MS = 350;
    @Unique private static final long REPEAT_INTERVAL_MS = 70;

    @Unique private String lastServerKey = "";
    @Unique private int lastPresetSig;
    @Unique private boolean lastEnableCustomButtons;
    @Unique private long lastSyncCheckMs;

    @Unique private String lastSearchText = "";
    @Unique private boolean lastSearchCase;
    @Unique private int lastVisibleCount;
    @Unique private List<OrderedText> cachedSearchLines = Collections.emptyList();

    @Unique private int lastW;
    @Unique private int lastH;

    @Inject(method = "init", at = @At("TAIL"))
    private void initTail(CallbackInfo ci) {
        ChatUtilConfig cfg = ChatUtilClient.CONFIG;
        if (!cfg.enabled) return;
        theme = cfg.argb();

        int w = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int h = MinecraftClient.getInstance().getWindow().getScaledHeight();

        if (cfg.enableSearchBar) {
            ChatUtilConfig.Pos p = cfg.positions.getOrDefault("search", new ChatUtilConfig.Pos(6, 6));
            search = new DraggableTextFieldWidget(MinecraftClient.getInstance().textRenderer, p.x, p.y, 180, 14, Text.literal("Search chat"));
            search.setConfigKey("search");
            draggables.add(search);
            ((ScreenAccessor)(Object)this).chatutil$invokeAddDrawableChild(search);
            search.setFocused(false);
        }

        ChatUtilConfig.Pos sp = cfg.positions.getOrDefault("search_panel", new ChatUtilConfig.Pos(4, 54));
        searchPanel = new DraggableSearchResultsWidget(sp.x, sp.y, 220, 80);
        searchPanel.setConfigKey("search_panel");
        searchPanel.setThemeARGB(theme);
        ((ScreenAccessor)(Object)this).chatutil$invokeAddDrawableChild(searchPanel);
        draggables.add(searchPanel);
        searchPanel.setVisiblePanel(false);

        if (cfg.showEditButton) {
            editToggle = new DraggableButtonWidget(w - 92, 6, 86, 16, Text.literal(label()), b -> {
                editMode = !editMode;
                ((ButtonWidget)editToggle).setMessage(Text.literal(label()));
                applyEditable();
                savePositions();
            });
            editToggle.setEditable(false);
            editToggle.setThemeARGB(theme);
            ((ScreenAccessor)(Object)this).chatutil$invokeAddDrawableChild(editToggle);
        }

        ChatUtilConfig.Pos upP = cfg.positions.getOrDefault("up", new ChatUtilConfig.Pos(6, 30));
        ChatUtilConfig.Pos downP = cfg.positions.getOrDefault("down", new ChatUtilConfig.Pos(28, 30));
        up = new DraggableButtonWidget(upP.x, upP.y, 20, 18, Text.literal("▲"), b -> history(-1));
        down = new DraggableButtonWidget(downP.x, downP.y, 20, 18, Text.literal("▼"), b -> history(1));
        up.setConfigKey("up"); down.setConfigKey("down");
        up.setThemeARGB(theme); down.setThemeARGB(theme);
        ((ScreenAccessor)(Object)this).chatutil$invokeAddDrawableChild(up);
        ((ScreenAccessor)(Object)this).chatutil$invokeAddDrawableChild(down);
        draggables.add(up); draggables.add(down);

        ChatUtilConfig.Pos leftP = cfg.positions.getOrDefault("left", new ChatUtilConfig.Pos(50, 30));
        ChatUtilConfig.Pos rightP = cfg.positions.getOrDefault("right", new ChatUtilConfig.Pos(72, 30));
        left = new DraggableButtonWidget(leftP.x, leftP.y, 20, 18, Text.literal("◀"), b -> moveCaret(-1));
        right = new DraggableButtonWidget(rightP.x, rightP.y, 20, 18, Text.literal("▶"), b -> moveCaret(1));
        left.setConfigKey("left"); right.setConfigKey("right");
        left.setThemeARGB(theme); right.setThemeARGB(theme);
        ((ScreenAccessor)(Object)this).chatutil$invokeAddDrawableChild(left);
        ((ScreenAccessor)(Object)this).chatutil$invokeAddDrawableChild(right);
        draggables.add(left); draggables.add(right);

        if (cfg.enableCustomButtons) {
            presetsOverlay = new PresetButtonsOverlay((Screen)(Object)this, cfg, editMode, theme);
            attachMissingPresetDraggables();
        }

        lastServerKey = serverKey();
        lastPresetSig = computePresetSig(cfg);
        lastEnableCustomButtons = cfg.enableCustomButtons;
        lastSyncCheckMs = System.nanoTime() / 1_000_000L;

        applyEditable();
        focusChatField();
        refreshButtonsFromConfig();

        lastW = w;
        lastH = h;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderTail(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ChatUtilConfig cfg = ChatUtilClient.CONFIG;
        if (!cfg.enabled) return;

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int w = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int h = MinecraftClient.getInstance().getWindow().getScaledHeight();

        if (w != lastW || h != lastH) {
            for (Draggable d : draggables) {
                int dw = d.getWidth();
                int dh = d.getHeight();
                int x = d.getX();
                int y = d.getY();

                int leftSpace = x;
                int rightSpace = Math.max(0, lastW - (x + dw));
                int topSpace = y;
                int bottomSpace = Math.max(0, lastH - (y + dh));

                boolean anchorLeft = leftSpace <= rightSpace;
                boolean anchorTop = topSpace <= bottomSpace;

                int nx;
                int ny;

                if (anchorLeft) {
                    nx = (int)Math.round((x / (double)Math.max(1, lastW)) * w);
                } else {
                    int rd = rightSpace;
                    nx = w - (int)Math.round((rd / (double)Math.max(1, lastW)) * w) - dw;
                }

                if (anchorTop) {
                    ny = (int)Math.round((y / (double)Math.max(1, lastH)) * h);
                } else {
                    int bd = bottomSpace;
                    ny = h - (int)Math.round((bd / (double)Math.max(1, lastH)) * h) - dh;
                }

                if (nx < 0) nx = 0;
                if (ny < 0) ny = 0;
                if (nx > w - dw) nx = w - dw;
                if (ny > h - dh) ny = h - dh;

                d.setX(nx);
                d.setY(ny);
                String k = d.getConfigKey();
                if (k != null) cfg.positions.put(k, new ChatUtilConfig.Pos(nx, ny));
            }
            if (editToggle != null) editToggle.setX(w - 92);
            lastW = w;
            lastH = h;
            cfg.layoutW = w;
            cfg.layoutH = h;
        }

        if (editToggle != null) editToggle.setX(w - 92);

        int newTheme = cfg.argb();
        if (newTheme != theme) {
            theme = newTheme;
            if (searchPanel != null) searchPanel.setThemeARGB(theme);
            if (editToggle != null) editToggle.setThemeARGB(theme);
            if (up != null) up.setThemeARGB(theme);
            if (down != null) down.setThemeARGB(theme);
            if (left != null) left.setThemeARGB(theme);
            if (right != null) right.setThemeARGB(theme);
            if (presetsOverlay != null) {
                for (DraggableButtonWidget b : presetsOverlay.all()) b.setThemeARGB(theme);
            }
        }

        long nowMs = System.nanoTime() / 1_000_000L;
        if (nowMs - lastSyncCheckMs >= 150) {
            boolean changedEnable = cfg.enableCustomButtons != lastEnableCustomButtons;
            boolean changedServer = !serverKey().equals(lastServerKey);
            int sig = computePresetSig(cfg);
            boolean changedPresets = sig != lastPresetSig;

            if (changedEnable || changedServer || changedPresets) {
                if (cfg.enableCustomButtons) {
                    if (presetsOverlay == null) {
                        presetsOverlay = new PresetButtonsOverlay((Screen)(Object)this, cfg, editMode, theme);
                        attachMissingPresetDraggables();
                    } else {
                        presetsOverlay.sync((Screen)(Object)this, cfg, editMode, theme);
                        attachMissingPresetDraggables();
                    }
                    setPresetButtonsVisible(true);
                } else {
                    setPresetButtonsVisible(false);
                }
                lastEnableCustomButtons = cfg.enableCustomButtons;
                lastServerKey = serverKey();
                lastPresetSig = sig;
            }
            lastSyncCheckMs = nowMs;
        }

        long win = MinecraftClient.getInstance().getWindow().getHandle();
        boolean nowLeftDown = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (editMode && cfg.showGrid) {
            int s = Math.max(4, cfg.gridSize);
            int g = cfg.gridARGB;
            for (int x = 0; x < w; x += s) ctx.fill(x, 0, x + 1, h, g);
            for (int y = 0; y < h; y += s) ctx.fill(0, y, w, y + 1, g);
            for (Draggable d : draggables) {
                if (d.isDragging()) {
                    int nx = mouseX - d.getDragDX();
                    int ny = mouseY - d.getDragDY();
                    int sX = Math.round(nx / (float)s) * s;
                    int sY = Math.round(ny / (float)s) * s;
                    sX = Math.max(0, Math.min(sX, w - d.getWidth()));
                    sY = Math.max(0, Math.min(sY, h - d.getHeight()));
                    d.setX(sX);
                    d.setY(sY);
                }
            }
        }

        if (leftDown && !nowLeftDown) {
            for (Draggable d : draggables) d.endDrag();
            savePositions();
        }
        leftDown = nowLeftDown;

        if (!editMode && nowLeftDown) {
            boolean overL = left != null && left.visible && left.isMouseOver(mouseX, mouseY);
            boolean overR = right != null && right.visible && right.isMouseOver(mouseX, mouseY);
            long now = nowMs;

            if (overL) {
                if (!leftRepeating) {
                    leftRepeating = true;
                    leftStartMs = now;
                    leftLastMs = now;
                } else if (now - leftStartMs >= INITIAL_DELAY_MS && now - leftLastMs >= REPEAT_INTERVAL_MS) {
                    moveCaret(-1);
                    leftLastMs = now;
                }
            } else {
                leftRepeating = false;
            }

            if (overR) {
                if (!rightRepeating) {
                    rightRepeating = true;
                    rightStartMs = now;
                    rightLastMs = now;
                } else if (now - rightStartMs >= INITIAL_DELAY_MS && now - rightLastMs >= REPEAT_INTERVAL_MS) {
                    moveCaret(1);
                    rightLastMs = now;
                }
            } else {
                rightRepeating = false;
            }
        } else {
            leftRepeating = false;
            rightRepeating = false;
        }

        List<OrderedText> lines;
        boolean hasQuery = false;
        if (search != null) {
            String q = search.getText();
            hasQuery = q != null && !q.isBlank();
            if (hasQuery) {
                int visCount = visibleCount();
                if (!q.equals(lastSearchText) || ChatUtilClient.CONFIG.searchCaseSensitive != lastSearchCase || visCount != lastVisibleCount) {
                    cachedSearchLines = filterVisible(q, ChatUtilClient.CONFIG.searchCaseSensitive);
                    lastSearchText = q;
                    lastSearchCase = ChatUtilClient.CONFIG.searchCaseSensitive;
                    lastVisibleCount = visCount;
                }
                lines = cachedSearchLines;
            } else {
                lines = Collections.emptyList();
            }
        } else {
            lines = Collections.emptyList();
        }

        boolean panelOn = editMode || hasQuery;
        if (searchPanel != null) {
            searchPanel.setLines(lines, tr, Math.max(160, w - 10), 20);
            searchPanel.setEditable(editMode);
            searchPanel.setVisiblePanel(panelOn);
            searchPanel.setActive(panelOn);
            searchPanel.setVisible(panelOn);
        }

        for (Draggable d : draggables) {
            if (d instanceof DraggableButtonWidget) {
                DraggableButtonWidget b = (DraggableButtonWidget)d;
                if (!b.visible) continue;
                String key = b.getConfigKey();
                boolean isPreset = key != null && key.startsWith("preset:");
                int col = isPreset
                        ? ((theme & 0x00FFFFFF) | (alphaForPresets(ChatUtilClient.CONFIG.presetButtonTransparency) << 24))
                        : theme;
                int x = b.getX(), y = b.getY(), bw = b.getWidth(), bh = b.getHeight();
                ctx.fill(x, y, x + bw, y + 1, col);
                ctx.fill(x, y + bh - 1, x + bw, y + bh, col);
                ctx.fill(x, y, x + 1, y + bh, col);
                ctx.fill(x + bw - 1, y, x + bw, y + bh, col);
            }
        }

        if (search != null) {
            int sx = search.getX(), sy = search.getY(), sw = search.getWidth(), sh = search.getHeight();
            ctx.fill(sx, sy, sx + sw, sy + 1, theme);
            ctx.fill(sx, sy + sh - 1, sx + sw, sy + sh, theme);
            ctx.fill(sx, sy, sx + 1, sy + sh, theme);
            ctx.fill(sx + sw - 1, sy, sx + sw, sy + sh, theme);
        }

        refreshButtonsFromConfig();
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void mouseClickedTail(double mx, double my, int button, CallbackInfoReturnable<Boolean> cir) {
        ChatUtilConfig cfg = ChatUtilClient.CONFIG;
        if (!cfg.enabled) return;
        boolean clickSearch = search != null && search.isMouseOver(mx, my);
        if (!clickSearch) focusChatField();
        if (searchPanel != null) searchPanel.beginDrag((int)mx, (int)my);
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void removedTail(CallbackInfo ci) { if (ChatUtilClient.CONFIG.enabled) savePositions(); }

    @Unique
    private void focusChatField() {
        TextFieldWidget chat = ((ChatScreenAccessor)(Object)this).chatutil$getChatField();
        if (chat != null) {
            chat.setFocused(true);
            this.setFocused(chat);
            this.setInitialFocus(chat);
        }
        if (search != null) search.setFocused(false);
    }

    @Unique
    private void refreshButtonsFromConfig() {
        ChatUtilConfig cfg = ChatUtilClient.CONFIG;
        boolean showPrevNext = cfg.enablePrevNextButtons;
        boolean showLR = cfg.enableLeftRightButtons;
        if (up != null) { up.setVisible(showPrevNext); up.setActive(showPrevNext); }
        if (down != null) { down.setVisible(showPrevNext); down.setActive(showPrevNext); }
        if (left != null) { left.setVisible(showLR); left.setActive(showLR); }
        if (right != null) { right.setVisible(showLR); right.setActive(showLR); }
    }

    @Unique
    private void history(int dir) {
        List<String> sent = MinecraftClient.getInstance().inGameHud.getChatHud().getMessageHistory();
        if (sent.isEmpty()) return;
        TextFieldWidget chat = ((ChatScreenAccessor)(Object)this).chatutil$getChatField();
        if (chat == null) return;
        int idx = sent.indexOf(chat.getText());
        if (idx < 0) idx = sent.size();
        idx += dir;
        if (idx < 0) idx = 0;
        if (idx >= sent.size()) idx = sent.size() - 1;
        chat.setText(sent.get(idx));
        focusChatField();
    }

    @Unique
    private void moveCaret(int dx){
        TextFieldWidget chat = ((ChatScreenAccessor)(Object)this).chatutil$getChatField();
        if (chat == null) return;
        int p = chat.getCursor() + dx;
        p = Math.max(0, Math.min(p, chat.getText().length()));
        chat.setCursor(p, false);
        chat.setSelectionStart(p);
        chat.setSelectionEnd(p);
        focusChatField();
    }

    @Unique
    private List<OrderedText> filterVisible(String query, boolean caseSensitive) {
        List<OrderedText> out = new ArrayList<>();
        List<ChatHudLine.Visible> vis = ((ChatHudAccessor)MinecraftClient.getInstance().inGameHud.getChatHud()).getVisibleMessages();
        if (vis == null) return out;
        String q = caseSensitive ? query : query.toLowerCase(Locale.ROOT);
        for (ChatHudLine.Visible v : vis) {
            OrderedText ot = v.content();
            StringBuilder sb = new StringBuilder();
            ot.accept((i, style, cp) -> { sb.appendCodePoint(cp); return true; });
            String s = sb.toString();
            String cmp = caseSensitive ? s : s.toLowerCase(Locale.ROOT);
            if (cmp.contains(q)) out.add(ot);
        }
        return out;
    }

    @Unique private void applyEditable(){ for (Draggable d : draggables) d.setEditable(editMode); if (presetsOverlay != null) presetsOverlay.setEditable(editMode); }
    @Unique private void savePositions(){ ChatUtilConfig cfg = ChatUtilClient.CONFIG; for (Draggable d : draggables) { String k = d.getConfigKey(); if (k != null) cfg.positions.put(k, new ChatUtilConfig.Pos(d.getX(), d.getY())); } if (presetsOverlay != null) presetsOverlay.save(cfg); cfg.layoutW = MinecraftClient.getInstance().getWindow().getScaledWidth(); cfg.layoutH = MinecraftClient.getInstance().getWindow().getScaledHeight(); cfg.save(); }
    @Unique private String label(){ return editMode ? "Edit Mode: ON" : "Edit Mode: OFF"; }

    @Unique
    private void attachMissingPresetDraggables() {
        if (presetsOverlay == null) return;
        for (DraggableButtonWidget b : presetsOverlay.all()) {
            if (!draggables.contains(b)) draggables.add(b);
        }
    }

    @Unique
    private void setPresetButtonsVisible(boolean show) {
        if (presetsOverlay == null) return;
        for (DraggableButtonWidget b : presetsOverlay.all()) {
            b.setActive(show);
            b.setVisible(show);
        }
    }

    @Unique
    private String serverKey() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.isInSingleplayer()) return "sp";
        ServerInfo info = mc.getCurrentServerEntry();
        if (info == null || info.address == null) return "";
        return info.address.trim().toLowerCase(Locale.ROOT);
    }

    @Unique
    private int computePresetSig(ChatUtilConfig cfg) {
        int sig = 1;
        sig = 31 * sig + (cfg.enableCustomButtons ? 1 : 0);
        sig = 31 * sig + cfg.presetButtonTransparency;
        for (ChatUtilConfig.PresetButton pb : cfg.presets) {
            sig = 31 * sig + (pb.enabled ? 1 : 0);
            sig = 31 * sig + (pb.label == null ? 0 : pb.label.hashCode());
            sig = 31 * sig + (pb.icon == null ? 0 : pb.icon.hashCode());
            sig = 31 * sig + pb.widthUnits;
            sig = 31 * sig + (pb.runOnClick ? 1 : 0);
            sig = 31 * sig + (pb.server == null ? 0 : pb.server.trim().toLowerCase(Locale.ROOT).hashCode());
        }
        return sig;
    }

    @Unique
    private int visibleCount() {
        List<ChatHudLine.Visible> vis = ((ChatHudAccessor)MinecraftClient.getInstance().inGameHud.getChatHud()).getVisibleMessages();
        return vis == null ? 0 : vis.size();
    }

    @Unique
    private int alphaForPresets(int percent){
        int p = Math.max(0, Math.min(100, percent));
        return (int)Math.round(255.0 * (100 - p) / 100.0);
    }
}
