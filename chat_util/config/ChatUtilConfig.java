package me.cutebow.chat_util.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class ChatUtilConfig {
    public boolean enabled = true;
    public boolean enableSearchBar = true;
    public boolean enablePrevNextButtons = true;
    public boolean enableLeftRightButtons = true;
    public boolean enableCustomButtons = true;
    public boolean showEditButton = true;
    public boolean searchCaseSensitive = false;
    public boolean showGrid = true;
    public int gridSize = 8;
    public int themeARGB = 0xFF66CCFF;
    public int gridARGB = 0x40FFFFFF;

    public int presetButtonTransparency = -1;
    public int buttonAlpha = 220;

    public int layoutW = 0;
    public int layoutH = 0;

    public Map<String, Pos> positions = new HashMap<>();
    public List<PresetButton> presets = new ArrayList<>();

    public static class Pos { public int x,y; public Pos(){} public Pos(int x,int y){this.x=x;this.y=y;} }

    public static class PresetButton {
        public int id;
        public boolean enabled = false;
        public String label = "Button";
        public String command = "";
        public String icon = "★";
        public int widthUnits = 4;
        public boolean runOnClick = false;
        public String server = "";
    }

    public int argb(){ return themeARGB; }

    public void save(){
        try {
            File f = new File(MinecraftClient.getInstance().runDirectory, "config/chat_util.json");
            f.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(f)) { new GsonBuilder().setPrettyPrinting().create().toJson(this, w); }
        } catch (Exception ignored) {}
    }

    public static ChatUtilConfig load(){
        ChatUtilConfig cfg;
        File f = new File(MinecraftClient.getInstance().runDirectory, "config/chat_util.json");
        boolean exists = f.exists();
        try {
            if (exists) {
                try (FileReader r = new FileReader(f)) { cfg = new Gson().fromJson(r, ChatUtilConfig.class); }
            } else {
                cfg = defaults();
            }
        } catch (Exception e) { cfg = defaults(); exists = false; }

        if (cfg.presets.isEmpty()) {
            for (int i=0;i<20;i++){ PresetButton pb = new PresetButton(); pb.id=i; pb.label="Btn "+(i+1); pb.icon=""; pb.widthUnits=4; pb.enabled=false; cfg.presets.add(pb); }
        } else {
            for (int i=0;i<cfg.presets.size();i++) if (cfg.presets.get(i).id!=i) cfg.presets.get(i).id=i;
            while (cfg.presets.size()<20){ PresetButton pb = new PresetButton(); pb.id=cfg.presets.size(); pb.enabled=false; cfg.presets.add(pb); }
            if (cfg.presets.size()>20) cfg.presets = cfg.presets.subList(0,20);
        }
        if (cfg.positions == null) cfg.positions = new HashMap<>();

        if (exists) {
            if (cfg.presetButtonTransparency < 0 || cfg.presetButtonTransparency > 100) {
                int mapped = 100 - Math.round(cfg.buttonAlpha * 100f / 255f);
                if (mapped < 0) mapped = 0;
                if (mapped > 100) mapped = 100;
                cfg.presetButtonTransparency = mapped;
            }
            if (cfg.buttonAlpha < 0 || cfg.buttonAlpha > 255) cfg.buttonAlpha = 220;
        }

        for (PresetButton pb : cfg.presets) {
            if (pb.widthUnits < 1) pb.widthUnits = 1;
            if (pb.widthUnits > 12) pb.widthUnits = 12;
        }
        return cfg;
    }

    private static ChatUtilConfig defaults(){
        ChatUtilConfig c = new ChatUtilConfig();
        c.enabled = true;
        c.enableSearchBar = true;
        c.enablePrevNextButtons = true;
        c.enableLeftRightButtons = true;
        c.enableCustomButtons = true;
        c.showEditButton = true;
        c.searchCaseSensitive = false;
        c.showGrid = true;
        c.gridSize = 12;
        c.themeARGB = 0xFF474747;
        c.gridARGB = 0x4000FFF9;
        c.presetButtonTransparency = 75;
        c.buttonAlpha = 64;

        c.positions.put("down", new Pos(24, 444));
        c.positions.put("preset:14", new Pos(5482, 54));
        c.positions.put("preset:15", new Pos(5556, 54));
        c.positions.put("search", new Pos(0, 240));
        c.positions.put("preset:12", new Pos(5334, 54));
        c.positions.put("preset:13", new Pos(5408, 54));
        c.positions.put("preset:10", new Pos(5186, 54));
        c.positions.put("preset:11", new Pos(5260, 54));
        c.positions.put("up", new Pos(0, 444));
        c.positions.put("right", new Pos(72, 444));
        c.positions.put("preset:18", new Pos(5778, 54));
        c.positions.put("preset:19", new Pos(5852, 54));
        c.positions.put("preset:16", new Pos(5630, 54));
        c.positions.put("preset:17", new Pos(5704, 54));
        c.positions.put("preset:2", new Pos(800, 400));
        c.positions.put("preset:1", new Pos(800, 424));
        c.positions.put("preset:0", new Pos(800, 448));
        c.positions.put("left", new Pos(48, 444));
        c.positions.put("preset:9", new Pos(5112, 54));
        c.positions.put("preset:8", new Pos(5038, 54));
        c.positions.put("preset:7", new Pos(4964, 54));
        c.positions.put("preset:6", new Pos(4890, 54));
        c.positions.put("preset:5", new Pos(4816, 54));
        c.positions.put("preset:4", new Pos(4742, 54));
        c.positions.put("preset:3", new Pos(782, 360));
        c.positions.put("search_panel", new Pos(12, 48));

        for (int i=0;i<20;i++){
            PresetButton pb = new PresetButton();
            pb.id = i;
            pb.enabled = false;
            pb.label = "Btn "+(i+1);
            pb.command = "";
            pb.icon = "";
            pb.widthUnits = 4;
            pb.runOnClick = false;
            pb.server = "";
            c.presets.add(pb);
        }

        c.presets.get(0).enabled = true;
        c.presets.get(0).label = "name";
        c.presets.get(0).command = "/yourcommand";
        c.presets.get(0).icon = "★";
        c.presets.get(0).widthUnits = 3;

        c.presets.get(1).enabled = true;
        c.presets.get(1).label = "name";
        c.presets.get(1).command = "/yourcommand";
        c.presets.get(1).icon = "⚐";
        c.presets.get(1).widthUnits = 3;

        c.presets.get(2).enabled = true;
        c.presets.get(2).label = "name";
        c.presets.get(2).command = "/yourcommand";
        c.presets.get(2).icon = "✉";
        c.presets.get(2).widthUnits = 3;

        return c;
    }
}
