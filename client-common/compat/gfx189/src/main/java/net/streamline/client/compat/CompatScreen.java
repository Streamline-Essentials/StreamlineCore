package net.streamline.client.compat;

import net.minecraft.client.gui.GuiScreen;
import net.streamline.client.Canvas;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;

/**
 * A {@link GuiScreen} whose drawing and input arrive through version-agnostic callbacks, so
 * Streamline's screens are written once. This is Minecraft 1.8.9's.
 *
 * <p>Key codes reach {@link #key} as GLFW codes, as on every other version; LWJGL 2's codes
 * are translated for the keys the screens use. Escape that no callback takes calls
 * {@link #onClose()}, as a modern screen does.</p>
 */
public abstract class CompatScreen extends GuiScreen {

    /** GLFW key codes the shared screens act on, by LWJGL 2 key code. */
    private static final int[][] KEYS = {
            {Keyboard.KEY_ESCAPE, 256},
            {Keyboard.KEY_RETURN, 257},
            {Keyboard.KEY_BACK, 259},
            {Keyboard.KEY_NUMPADENTER, 335},
            {Keyboard.KEY_DELETE, 261},
            {Keyboard.KEY_RIGHT, 262},
            {Keyboard.KEY_LEFT, 263},
            {Keyboard.KEY_HOME, 268},
            {Keyboard.KEY_END, 269},
            {Keyboard.KEY_R, 82},
            {Keyboard.KEY_V, 86},
    };

    /** GLFW's modifier bits, which {@link #key} receives as on every other version. */
    private static final int MOD_SHIFT = 1;
    private static final int MOD_CONTROL = 2;

    private final String title;
    private int lastMouseX;
    private int lastMouseY;

    protected CompatScreen(String title) {
        this.title = title;
    }

    public String getTitleText() {
        return title;
    }

    /** Draws the whole screen; nothing is drawn behind it but the game. */
    protected abstract void draw(Canvas canvas, int mouseX, int mouseY, float partialTick);

    protected boolean mouseDown(double x, double y, int button) {
        return false;
    }

    protected boolean mouseUp(double x, double y, int button) {
        return false;
    }

    protected boolean mouseDrag(double x, double y, int button, double dx, double dy) {
        return false;
    }

    /** @param amount positive when scrolling up, negative when scrolling down */
    protected boolean scroll(double x, double y, double amount) {
        return false;
    }

    /** @param key a GLFW key code */
    protected boolean key(int key, int modifiers) {
        return false;
    }

    protected boolean typed(String text) {
        return false;
    }

    /** Tells the game whether a text field of this screen has focus; 1.8.9 always sends characters. */
    protected void textInput(boolean focused) {
    }

    /** @return the system clipboard's text, empty when it holds none */
    protected String clipboard() {
        String text = getClipboardString();
        return text == null ? "" : text;
    }

    /** Leaves the screen; the default closes it. */
    public void onClose() {
        mc.displayGuiScreen(null);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        draw(GfxCanvas.current(), mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (! mouseDown(mouseX, mouseY, button)) super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        if (! mouseUp(mouseX, mouseY, button)) super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long sinceClick) {
        double dx = mouseX - lastMouseX;
        double dy = mouseY - lastMouseY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (! mouseDrag(mouseX, mouseY, button, dx, dy)) super.mouseClickMove(mouseX, mouseY, button, sinceClick);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        double x = Mouse.getEventX() * (double) width / mc.displayWidth;
        double y = height - Mouse.getEventY() * (double) height / mc.displayHeight - 1;
        scroll(x, y, wheel > 0 ? 1.0 : -1.0);
    }

    @Override
    protected void keyTyped(char character, int keyCode) throws IOException {
        int modifiers = (isShiftKeyDown() ? MOD_SHIFT : 0) | (isCtrlKeyDown() ? MOD_CONTROL : 0);
        // A printable character goes to a focused field before the key does, so letters the
        // screens also bind (R, V) still type. Control with a letter arrives as a control
        // character, so shortcuts reach key(); AltGr's characters are printable and type.
        if (character >= ' ' && character != 127 && typed(String.valueOf(character))) return;
        int glfw = toGlfw(keyCode);
        if (glfw != -1 && key(glfw, modifiers)) return;
        if (keyCode == Keyboard.KEY_ESCAPE) {
            onClose();
            return;
        }
        super.keyTyped(character, keyCode);
    }

    private static int toGlfw(int lwjglKey) {
        for (int[] pair : KEYS) {
            if (pair[0] == lwjglKey) return pair[1];
        }
        return -1;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
