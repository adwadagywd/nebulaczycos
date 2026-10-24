package pl.nebula.module;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import pl.nebula.render.Gfx;
import pl.nebula.setting.Setting.*;
import pl.nebula.util.Targets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Nametags extends Module {
    public final NumberSetting scale = add(new NumberSetting("Scale", 1.0, 0.5, 4.0, 0.1));
    public final ColorSetting color = add(new ColorSetting("Name Color", 0.0f, 0.0f));
    public final BoolSetting showDistance = add(new BoolSetting("Distance", true));
    public final BoolSetting showHealth = add(new BoolSetting("Health", true));
    public final BoolSetting showEnchants = add(new BoolSetting("Enchants", true));
    public final BoolSetting showSet = add(new BoolSetting("Armor Set", true));
    public final BoolSetting background = add(new BoolSetting("Background", true));
    public final NumberSetting range = add(new NumberSetting("Max Distance", 128, 8, 512, 8));
    public final BoolSetting invisible = add(new BoolSetting("Invisible Players", true));
    public final BoolSetting self = add(new BoolSetting("Self (in Freecam)", true));
    public final BoolSetting hideVanilla = add(new BoolSetting("Hide Vanilla Tag", true));

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final EquipmentSlot[] ENCH_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] ENCH_LABEL = {"Main", "Off", "Helm", "Chest", "Legs", "Boots"};

    public Nametags() {
        super("Nametags", "Custom, always-visible nametags");
    }

    @Override
    public void render3d(Gfx g) {
        List<PlayerEntity> players = Targets.players(g.cam, range.d(), invisible.on(), self.on());
        if (players.isEmpty()) return;

        TextRenderer tr = mc.textRenderer;
        VertexConsumerProvider.Immediate imm = mc.getBufferBuilders().getEntityVertexConsumers();
        double gui = mc.getWindow().getScaleFactor();

        for (PlayerEntity p : players) {
            Vec3d rel = p.getLerpedPos(g.tickDelta).add(0, p.getHeight() + 0.35, 0).subtract(g.cam);
            double camDist = Math.max(0.5, rel.length());

            // constant on-screen size: world size grows with distance
            float s = (float) (g.pxFactor * camDist * scale.d() * gui);

            MatrixStack ms = new MatrixStack();
            ms.multiplyPositionMatrix(g.pos);
            ms.translate(rel.x, rel.y, rel.z);
            ms.multiply(g.camera.getRotation());
            ms.scale(s, -s, s);
            Matrix4f m = ms.peek().getPositionMatrix();

            List<Text> lines = buildLines(p);
            int bg = background.on() ? 0x66000000 : 0;
            float y = -lines.size() * 10f;
            for (Text line : lines) {
                float x = -tr.getWidth(line) / 2f;
                tr.draw(line, x, y, 0xFFFFFFFF, true, m, imm, TextRenderer.TextLayerType.SEE_THROUGH, bg, 0xF000F0);
                y += 10f;
            }
        }
        imm.draw();
    }

    private List<Text> buildLines(PlayerEntity p) {
        List<Text> out = new ArrayList<>();

        MutableText main = Text.literal(p.getName().getString()).styled(st -> st.withColor(color.rgb()));
        if (showDistance.on() && mc.player != null) {
            main.append(Text.literal(String.format(Locale.ROOT, " [%.0fm]", mc.player.distanceTo(p)))
                    .styled(st -> st.withColor(0xAAAAAA)));
        }
        if (showHealth.on()) {
            float hp = p.getHealth() + p.getAbsorptionAmount();
            float frac = MathHelper.clamp(hp / Math.max(1f, p.getMaxHealth()), 0f, 1f);
            int hc = MathHelper.hsvToRgb(frac * 0.33f, 1f, 1f) & 0xFFFFFF;
            main.append(Text.literal(String.format(Locale.ROOT, " %.1f HP", hp)).styled(st -> st.withColor(hc)));
        }
        out.add(main);

        if (showSet.on()) out.add(Text.literal(armorSet(p)).styled(st -> st.withColor(0x55FFFF)));

        if (showEnchants.on()) {
            for (int i = 0; i < ENCH_SLOTS.length; i++) {
                String e = enchants(p.getEquippedStack(ENCH_SLOTS[i]));
                if (!e.isEmpty()) {
                    out.add(Text.literal(ENCH_LABEL[i] + ": " + e).styled(st -> st.withColor(0xFF99FF)));
                }
            }
        }
        return out;
    }

    /** Determines the dominant armor material from the item ids (works for any mod-less vanilla set). */
    private String armorSet(PlayerEntity p) {
        Map<String, Integer> counts = new HashMap<>();
        for (EquipmentSlot slot : ARMOR) {
            ItemStack st = p.getEquippedStack(slot);
            if (st.isEmpty()) continue;
            String path = Registries.ITEM.getId(st.getItem()).getPath();
            int idx = path.lastIndexOf('_');
            String material = idx > 0 ? path.substring(0, idx) : path;
            counts.merge(material, 1, Integer::sum);
        }
        if (counts.isEmpty()) return "Set: none";
        String best = null;
        int bc = 0;
        for (var e : counts.entrySet()) {
            if (e.getValue() > bc) { best = e.getKey(); bc = e.getValue(); }
        }
        return "Set: " + Character.toUpperCase(best.charAt(0)) + best.substring(1) + " " + bc + "/4";
    }

    private String enchants(ItemStack stack) {
        if (stack.isEmpty()) return "";
        ItemEnchantmentsComponent comp = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        if (comp.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Object2IntMap.Entry<?> e : comp.getEnchantmentEntries()) {
            Object key = e.getKey();
            String path = "?";
            if (key instanceof net.minecraft.registry.entry.RegistryEntry<?> re) {
                path = re.getKey().map(k -> k.getValue().getPath()).orElse("?");
            }
            if (sb.length() > 0) sb.append(' ');
            sb.append(abbr(path)).append(e.getIntValue());
        }
        return sb.toString();
    }

    private static String abbr(String path) {
        String[] parts = path.split("_");
        if (parts.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (String s : parts) if (!s.isEmpty()) sb.append(Character.toUpperCase(s.charAt(0)));
            return sb.toString();
        }
        return Character.toUpperCase(path.charAt(0)) + path.substring(1, Math.min(path.length(), 4));
    }
}
