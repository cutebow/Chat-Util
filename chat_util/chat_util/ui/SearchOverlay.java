package me.cutebow.chat_util.ui;

import me.cutebow.chat_util.config.ChatUtilConfig;
import me.cutebow.chat_util.mixin.ChatHudAccessor;
import me.cutebow.chat_util.mixin.ScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SearchOverlay {
    private final DraggableTextFieldWidget field;
    private final ChatUtilConfig cfg;

    public SearchOverlay(Screen screen, ChatUtilConfig cfg) {
        this.cfg = cfg;
        ChatUtilConfig.Pos p = cfg.positions.getOrDefault("search", new ChatUtilConfig.Pos(6, 6));
        this.field = new DraggableTextFieldWidget(MinecraftClient.getInstance().textRenderer, p.x, p.y, 180, 14, Text.literal("Search chat"));
        this.field.setConfigKey("search");
        this.field.setFocused(false);
        ((ScreenAccessor)(Object)screen).chatutil$invokeAddDrawableChild(field);
    }

    public DraggableTextFieldWidget field(){ return field; }

    public void render(Screen screen, DrawContext ctx, int theme) {
        if (!field.isFocused()) return;
        String q = field.getText();
        if (q.isBlank()) return;

        List<OrderedText> filtered = filterVisible(q, cfg.searchCaseSensitive);
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        int lh = 9;
        int lines = Math.min(filtered.size(), 20);
        int boxH = lines * lh + 6;
        int boxW = Math.min(320, screen.width - 10);
        int x = 4, y = screen.height - 28 - boxH;
        ctx.fill(x - 2, y - 2, x - 2 + boxW, y - 2 + boxH, 0xCC000000);
        for (int i = 0; i < lines; i++) {
            ctx.drawText(tr, filtered.get(filtered.size() - 1 - i), x, y + i * lh, 0xFFFFFF, true);
        }

        // Hehehehemoemwomeomwomw cutebow is pro
        ctx.fill(field.getX() - 1, field.getY() - 1, field.getX() + field.getWidth() + 1, field.getY(), theme);
        ctx.fill(field.getX() - 1, field.getY() + field.getHeight(), field.getX() + field.getWidth() + 1, field.getY() + field.getHeight() + 1, theme);
        ctx.fill(field.getX() - 1, field.getY(), field.getX(), field.getY() + field.getHeight(), theme);
        ctx.fill(field.getX() + field.getWidth(), field.getY(), field.getX() + field.getWidth() + 1, field.getY() + field.getHeight(), theme);
    }

    public void save(ChatUtilConfig cfg){
        cfg.positions.put("search", new ChatUtilConfig.Pos(field.getX(), field.getY()));
    }

    private List<OrderedText> filterVisible(String query, boolean caseSensitive) {
        List<OrderedText> out = new ArrayList<>();
        List<net.minecraft.client.gui.hud.ChatHudLine.Visible> vis =
                ((ChatHudAccessor)MinecraftClient.getInstance().inGameHud.getChatHud()).getVisibleMessages();
        String q = caseSensitive ? query : query.toLowerCase(Locale.ROOT);
        for (net.minecraft.client.gui.hud.ChatHudLine.Visible v : vis) {
            OrderedText ot = v.content();
            StringBuilder sb = new StringBuilder();
            ot.accept((idx, style, cp) -> { sb.appendCodePoint(cp); return true; });
            String s = sb.toString();
            String cmp = caseSensitive ? s : s.toLowerCase(Locale.ROOT);
            if (cmp.contains(q)) out.add(ot);
        }
        return out;
    }
}
