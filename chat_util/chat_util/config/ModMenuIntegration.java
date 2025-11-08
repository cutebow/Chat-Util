package me.cutebow.chat_util.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() { return this::build; }

    private Screen build(Screen parent){
        ChatUtilConfig cfg = me.cutebow.chat_util.ChatUtilClient.CONFIG;

        List<Option<?>> layout = new ArrayList<>();
        layout.add(Option.<Boolean>createBuilder().name(Text.literal("Enable Mod"))
                .binding(true, () -> cfg.enabled, v -> cfg.enabled = v)
                .controller(TickBoxControllerBuilder::create).build());
        layout.add(Option.<Boolean>createBuilder().name(Text.literal("Enable Search Bar"))
                .binding(true, () -> cfg.enableSearchBar, v -> cfg.enableSearchBar = v)
                .controller(TickBoxControllerBuilder::create).build());
        layout.add(Option.<Boolean>createBuilder().name(Text.literal("Prev/Next Buttons"))
                .binding(true, () -> cfg.enablePrevNextButtons, v -> cfg.enablePrevNextButtons = v)
                .controller(TickBoxControllerBuilder::create).build());
        layout.add(Option.<Boolean>createBuilder().name(Text.literal("Left/Right Buttons"))
                .binding(true, () -> cfg.enableLeftRightButtons, v -> cfg.enableLeftRightButtons = v)
                .controller(TickBoxControllerBuilder::create).build());
        layout.add(Option.<Boolean>createBuilder().name(Text.literal("Show Edit Toggle Button"))
                .binding(true, () -> cfg.showEditButton, v -> cfg.showEditButton = v)
                .controller(TickBoxControllerBuilder::create).build());
        layout.add(Option.<Boolean>createBuilder().name(Text.literal("Show Grid in Edit Mode"))
                .binding(true, () -> cfg.showGrid, v -> cfg.showGrid = v)
                .controller(TickBoxControllerBuilder::create).build());
        layout.add(Option.<Integer>createBuilder().name(Text.literal("Grid Size"))
                .binding(8, () -> cfg.gridSize, v -> cfg.gridSize = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(4, 32).step(1)).build());
        layout.add(Option.<Boolean>createBuilder().name(Text.literal("Case Sensitive Search"))
                .binding(false, () -> cfg.searchCaseSensitive, v -> cfg.searchCaseSensitive = v)
                .controller(TickBoxControllerBuilder::create).build());
        layout.add(Option.<Integer>createBuilder().name(Text.literal("Preset Button Transparency"))
                .binding(15, () -> cfg.presetButtonTransparency, v -> cfg.presetButtonTransparency = Math.max(0, Math.min(100, v)))
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 100).step(1)).build());
        layout.add(Option.<Color>createBuilder().name(Text.literal("Theme Color"))
                .binding(new Color(0xFF66CCFF, true),
                        () -> new Color(cfg.themeARGB, true),
                        v -> cfg.themeARGB = v.getRGB())
                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build());
        layout.add(Option.<Color>createBuilder().name(Text.literal("Grid Color"))
                .binding(new Color(0x40FFFFFF, true),
                        () -> new Color(cfg.gridARGB, true),
                        v -> cfg.gridARGB = v.getRGB())
                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build());

        ButtonOption reset = ButtonOption.createBuilder()
                .name(Text.literal("Reset Button Positions"))
                .action((screen, option) -> {
                    resetPositions(cfg);
                    cfg.save();
                })
                .build();
        layout.add(reset);

        layout.add(Option.<Boolean>createBuilder().name(Text.literal("Enable Custom Buttons"))
                .binding(true, () -> cfg.enableCustomButtons, v -> cfg.enableCustomButtons = v)
                .controller(TickBoxControllerBuilder::create).build());

        List<OptionGroup> groups = new ArrayList<>();
        for (int i = 0; i < cfg.presets.size(); i++) {
            ChatUtilConfig.PresetButton pb = cfg.presets.get(i);

            Option<Boolean> enabled = Option.<Boolean>createBuilder().name(Text.literal("Enabled"))
                    .binding(pb.enabled, ()->pb.enabled, v -> pb.enabled = v).controller(TickBoxControllerBuilder::create).build();
            Option<String> label = Option.<String>createBuilder().name(Text.literal("Label"))
                    .binding(pb.label, ()->pb.label, v -> pb.label = v).controller(StringControllerBuilder::create).build();
            Option<String> cmd = Option.<String>createBuilder().name(Text.literal("Command (/ or text)"))
                    .binding(pb.command, ()->pb.command, v -> pb.command = v).controller(StringControllerBuilder::create).build();
            Option<String> icon = Option.<String>createBuilder().name(Text.literal("Icon"))
                    .binding(pb.icon, ()->pb.icon, v -> pb.icon = v).controller(StringControllerBuilder::create).build();
            Option<Integer> width = Option.<Integer>createBuilder().name(Text.literal("Width Units"))
                    .binding(pb.widthUnits, ()->pb.widthUnits, v -> pb.widthUnits = Math.max(1, Math.min(12, v)))
                    .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 12).step(1)).build();
            Option<String> server = Option.<String>createBuilder().name(Text.literal("Server Filter (IP)"))
                    .description(OptionDescription.of(Text.literal("Leave blank to show everywhere. Enter a server host like play.example.net, optionally with :port.")))
                    .binding(pb.server, ()->pb.server, v -> pb.server = v).controller(StringControllerBuilder::create).build();

            ButtonOption iconPicker = ButtonOption.createBuilder()
                    .name(Text.literal("Pick Icon"))
                    .action((screen, option) -> {
                        Screen current = MinecraftClient.getInstance().currentScreen;
                        Consumer<String> apply = s -> { icon.requestSet(s); pb.icon = s; };
                        MinecraftClient.getInstance().setScreen(new me.cutebow.chat_util.ui.IconPickerScreen(current, apply));
                    })
                    .build();

            groups.add(OptionGroup.createBuilder().name(Text.literal("Button "+(i+1)))
                    .options(List.of(enabled, label, cmd, icon, iconPicker, width, server)).build());
        }

        List<ConfigCategory> cats = new ArrayList<>();
        cats.add(ConfigCategory.createBuilder().name(Text.literal("Layout")).options(layout).build());
        cats.add(ConfigCategory.createBuilder().name(Text.literal("Buttons")).groups(groups).build());

        return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Chat Util"))
                .save(cfg::save)
                .categories(cats)
                .build()
                .generateScreen(parent);
    }

    private void resetPositions(ChatUtilConfig cfg) {
        int w = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int h = MinecraftClient.getInstance().getWindow().getScaledHeight();

        cfg.positions.put("up", new ChatUtilConfig.Pos(0, Math.max(0, h - 36)));
        cfg.positions.put("down", new ChatUtilConfig.Pos(24, Math.max(0, h - 36)));
        cfg.positions.put("left", new ChatUtilConfig.Pos(48, Math.max(0, h - 36)));
        cfg.positions.put("right", new ChatUtilConfig.Pos(72, Math.max(0, h - 36)));

        cfg.positions.put("search", new ChatUtilConfig.Pos(0, Math.max(0, h / 2 - 7)));
        cfg.positions.put("search_panel", new ChatUtilConfig.Pos(12, 48));

        int colWidth = 160;
        int rowHeight = 22;
        int cols = Math.max(1, (w - 12) / colWidth);
        int startX = Math.max(6, w - colWidth);
        int startY = Math.max(6, h - rowHeight * 6);

        for (int i = 0; i < cfg.presets.size(); i++) {
            int col = i / 8;
            int row = i % 8;
            int x = Math.max(6, startX - col * colWidth);
            int y = Math.min(h - rowHeight, startY + row * rowHeight);
            cfg.positions.put("preset:" + i, new ChatUtilConfig.Pos(x, y));
        }

        cfg.layoutW = w;
        cfg.layoutH = h;
    }
}
