package pl.nebula.module;

import org.lwjgl.glfw.GLFW;
import pl.nebula.gui.ClickGuiScreen;

public class ClickGui extends Module {
    public ClickGui() {
        super("ClickGUI", "Opens the Nebula menu");
        setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override public boolean hasToggle() { return false; }

    @Override public void toggle() {
        if (mc.currentScreen instanceof ClickGuiScreen) mc.setScreen(null);
        else mc.setScreen(new ClickGuiScreen());
    }
}
