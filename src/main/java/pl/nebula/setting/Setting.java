package pl.nebula.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.math.MathHelper;

import java.util.Locale;
import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    public final String name;
    protected T value;
    private BooleanSupplier visible = () -> true;

    protected Setting(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public T get() { return value; }
    public void set(T v) { value = v; }
    public void visibleIf(BooleanSupplier s) { visible = s; }
    public boolean isVisible() { return visible.getAsBoolean(); }

    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement e);

    // ---------------------------------------------------------------- Bool
    public static class BoolSetting extends Setting<Boolean> {
        public BoolSetting(String name, boolean def) { super(name, def); }
        public boolean on() { return value; }
        public void toggle() { value = !value; }
        @Override public JsonElement toJson() { return new JsonPrimitive(value); }
        @Override public void fromJson(JsonElement e) { value = e.getAsBoolean(); }
    }

    // ---------------------------------------------------------------- Number
    public static class NumberSetting extends Setting<Double> {
        public final double min, max, step;

        public NumberSetting(String name, double def, double min, double max, double step) {
            super(name, def);
            this.min = min; this.max = max; this.step = step;
            set(def);
        }

        @Override public void set(Double v) {
            double c = Math.max(min, Math.min(max, v));
            if (step > 0) c = Math.round(c / step) * step;
            value = Math.max(min, Math.min(max, c));
        }

        public double d() { return value; }
        public float f() { return value.floatValue(); }
        public int i() { return (int) Math.round(value); }

        public String display() {
            return step >= 1 ? String.valueOf((int) Math.round(value)) : String.format(Locale.ROOT, "%.2f", value);
        }

        @Override public JsonElement toJson() { return new JsonPrimitive(value); }
        @Override public void fromJson(JsonElement e) { set(e.getAsDouble()); }
    }

    // ---------------------------------------------------------------- Enum
    public static class EnumSetting<E extends Enum<E>> extends Setting<E> {
        public EnumSetting(String name, E def) { super(name, def); }

        public void next() {
            E[] a = value.getDeclaringClass().getEnumConstants();
            value = a[(value.ordinal() + 1) % a.length];
        }

        public void prev() {
            E[] a = value.getDeclaringClass().getEnumConstants();
            value = a[(value.ordinal() - 1 + a.length) % a.length];
        }

        @Override public JsonElement toJson() { return new JsonPrimitive(value.name()); }

        @Override public void fromJson(JsonElement e) {
            for (E c : value.getDeclaringClass().getEnumConstants()) {
                if (c.name().equals(e.getAsString())) { value = c; return; }
            }
        }
    }

    // ---------------------------------------------------------------- Color (hue + saturation)
    public static class ColorSetting extends Setting<Float> {
        public float sat;

        public ColorSetting(String name, float hue, float sat) {
            super(name, hue);
            this.sat = sat;
        }

        public int rgb() { return MathHelper.hsvToRgb(value, sat, 1f) & 0xFFFFFF; }

        @Override public JsonElement toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("h", value);
            o.addProperty("s", sat);
            return o;
        }

        @Override public void fromJson(JsonElement e) {
            JsonObject o = e.getAsJsonObject();
            value = o.get("h").getAsFloat();
            sat = o.get("s").getAsFloat();
        }
    }
}
