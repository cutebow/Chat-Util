package me.cutebow.chat_util.ui;

import me.cutebow.chat_util.config.ChatUtilConfig;
import me.cutebow.chat_util.mixin.ScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

import java.util.*;

public final class PresetButtonsOverlay {
    private final List<DraggableButtonWidget> buttons = new ArrayList<>();
    private final ChatUtilConfig cfg;

    public PresetButtonsOverlay(Screen screen, ChatUtilConfig cfg, boolean editable, int theme) {
        this.cfg = cfg;
        for (ChatUtilConfig.PresetButton pb : cfg.presets) {
            if (!pb.enabled) continue;
            if (!isActiveForServer(pb)) continue;
            String baseKey = "preset:" + pb.id;
            String key = keyFor(pb);
            int bw = Math.max(1, pb.widthUnits) * 18;
            ChatUtilConfig.Pos pos = cfg.positions.containsKey(key)
                    ? cfg.positions.get(key)
                    : cfg.positions.getOrDefault(baseKey, new ChatUtilConfig.Pos(6 + buttons.size() * 74, 54));
            DraggableButtonWidget btn = new DraggableButtonWidget(pos.x, pos.y, bw, 18,
                    Text.literal((pb.icon == null || pb.icon.isEmpty() ? "" : pb.icon + " ") + pb.label), w -> {
                if (MinecraftClient.getInstance().player == null) return;
                String t = pb.command == null ? "" : pb.command;
                if (t.isEmpty()) return;
                if (pb.runOnClick || t.startsWith("/")) {
                    if (t.startsWith("/")) MinecraftClient.getInstance().player.networkHandler.sendChatCommand(t.substring(1));
                    else MinecraftClient.getInstance().player.networkHandler.sendChatMessage(t);
                } else {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(t);
                }
            });
            btn.setEditable(editable);
            btn.setThemeARGB(theme);
            btn.setPresetMode(true);
            btn.setAlpha(alphaFromTransparency(cfg.presetButtonTransparency));
            btn.setConfigKey(key);
            buttons.add(btn);
            ((ScreenAccessor)(Object)screen).chatutil$invokeAddDrawableChild(btn);
        }
    }

    public List<DraggableButtonWidget> all(){ return buttons; }

    public void setEditable(boolean editable){
        for (DraggableButtonWidget b : buttons) b.setEditable(editable);
    }

    public void render(net.minecraft.client.gui.DrawContext ctx, int theme){
        int a = alphaFromTransparency(cfg.presetButtonTransparency);
        int outline = (theme & 0x00FFFFFF) | (a << 24);
        for (DraggableButtonWidget b : buttons){
            int x=b.getX(), y=b.getY(), w=b.getWidth(), h=b.getHeight();
            ctx.fill(x - 1, y - 1, x + w + 1, y, outline);
            ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, outline);
            ctx.fill(x - 1, y, x, y + h, outline);
            ctx.fill(x + w, y, x + w + 1, y + h, outline);
        }
    }

    public void save(ChatUtilConfig cfg){
        for (DraggableButtonWidget b : buttons){
            String key = b.getConfigKey();
            if (key != null) cfg.positions.put(key, new ChatUtilConfig.Pos(b.getX(), b.getY()));
        }
    }

    public void sync(Screen screen, ChatUtilConfig cfg, boolean editable, int theme) {
        Map<String, DraggableButtonWidget> map = new HashMap<>();
        for (DraggableButtonWidget b : buttons) {
            map.put(b.getConfigKey(), b);
        }
        for (ChatUtilConfig.PresetButton pb : cfg.presets) {
            String baseKey = "preset:" + pb.id;
            String key = keyFor(pb);
            DraggableButtonWidget existing = map.get(key);
            if (existing == null) existing = map.get(baseKey);
            boolean allow = pb.enabled && isActiveForServer(pb);
            if (allow) {
                if (existing == null) {
                    int bw = Math.max(1, pb.widthUnits) * 18;
                    ChatUtilConfig.Pos pos = cfg.positions.containsKey(key)
                            ? cfg.positions.get(key)
                            : cfg.positions.getOrDefault(baseKey, new ChatUtilConfig.Pos(6 + buttons.size() * 74, 54));
                    DraggableButtonWidget btn = new DraggableButtonWidget(pos.x, pos.y, bw, 18,
                            Text.literal((pb.icon == null || pb.icon.isEmpty() ? "" : pb.icon + " ") + pb.label), w -> {
                        if (MinecraftClient.getInstance().player == null) return;
                        String t = pb.command == null ? "" : pb.command;
                        if (t.isEmpty()) return;
                        if (pb.runOnClick || t.startsWith("/")) {
                            if (t.startsWith("/")) MinecraftClient.getInstance().player.networkHandler.sendChatCommand(t.substring(1));
                            else MinecraftClient.getInstance().player.networkHandler.sendChatMessage(t);
                        } else {
                            MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(t);
                        }
                    });
                    btn.setEditable(editable);
                    btn.setThemeARGB(theme);
                    btn.setPresetMode(true);
                    btn.setAlpha(alphaFromTransparency(cfg.presetButtonTransparency));
                    btn.setConfigKey(key);
                    buttons.add(btn);
                    ((ScreenAccessor)(Object)screen).chatutil$invokeAddDrawableChild(btn);
                } else {
                    existing.setConfigKey(key);
                    int bw = Math.max(1, pb.widthUnits) * 18;
                    existing.setSize(bw, 18);
                    existing.setMessage(Text.literal((pb.icon == null || pb.icon.isEmpty() ? "" : pb.icon + " ") + pb.label));
                    existing.setActive(true);
                    existing.setVisible(true);
                    existing.setEditable(editable);
                    existing.setThemeARGB(theme);
                    existing.setPresetMode(true);
                    existing.setAlpha(alphaFromTransparency(cfg.presetButtonTransparency));
                }
            } else {
                if (existing != null) {
                    existing.setActive(false);
                    existing.setVisible(false);
                }
            }
        }
    }

    private boolean isActiveForServer(ChatUtilConfig.PresetButton pb) {
        if (pb.server == null || pb.server.isBlank()) return true;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.isInSingleplayer()) return false;
        ServerInfo info = mc.getCurrentServerEntry();
        if (info == null || info.address == null) return false;
        String a = normalize(info.address);
        String s = normalize(pb.server);
        return a.contains(s);
    }

    private String keyFor(ChatUtilConfig.PresetButton pb) {
        if (pb.server == null || pb.server.isBlank()) return "preset:" + pb.id;
        return "preset:" + pb.id + "@" + normalize(pb.server);
    }

    private String normalize(String s) { return s.trim().toLowerCase(java.util.Locale.ROOT); }

    private int alphaFromTransparency(int percent){
        int p = Math.max(0, Math.min(100, percent));
        return (int)Math.round(255.0 * (100 - p) / 100.0);
    }
}
