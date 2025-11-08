package me.cutebow.chat_util.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class IconPickerScreen extends Screen {
    private final Screen parent;
    private final Consumer<String> onPick;
    private static final String[] ICONS = new String[]{
            "★","☆","✦","✧","✪","✩","✔","✖","⚑","⚐","⚔","⛏","☠","✉","☄","❤","☀","☁","☂","♪",
            "◆","◇","■","□","▲","△","▶","▷","▼","▽","◀","◁","○","●","◈","◉","⬤","⬧","⬨","✿",
            "☾","☽","✈","⚙","⚡","❄","🔥","🌙","🌟","🔔","🔧","🔨","🗡","🛡","🏹","🧭","🧱","🧊","🍀","🍎"
    };

    public IconPickerScreen(Screen parent, Consumer<String> onPick) {
        super(Text.literal("Pick Icon"));
        this.parent = parent;
        this.onPick = onPick;
    }

    @Override
    protected void init() {
        int cols = 10;
        int bw = 28;
        int bh = 22;
        int gap = 4;
        int rows = (int)Math.ceil(ICONS.length / (double)cols);
        int gridW = cols * bw + (cols - 1) * gap;
        int gridH = rows * bh + (rows - 1) * gap;
        int sx = (this.width - gridW) / 2;
        int sy = (this.height - gridH) / 2;

        int i = 0;
        for (String s : ICONS) {
            int r = i / cols;
            int c = i % cols;
            int x = sx + c * (bw + gap);
            int y = sy + r * (bh + gap);
            ButtonWidget b = ButtonWidget.builder(Text.literal(s), btn -> {
                this.onPick.accept(s);
                MinecraftClient.getInstance().setScreen(parent);
            }).dimensions(x, y, bw, bh).build();
            this.addDrawableChild(b);
            i++;
        }

        ButtonWidget close = ButtonWidget.builder(Text.literal("Close"), btn -> MinecraftClient.getInstance().setScreen(parent))
                .dimensions(sx, sy + gridH + 12, 80, 20).build();
        this.addDrawableChild(close);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
