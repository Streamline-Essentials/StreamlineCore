package net.streamline.client.compat;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.streamline.client.Canvas;

/**
 * A {@link Screen} whose drawing and input arrive through version-agnostic callbacks, so
 * Streamline's screens are written once. This is Minecraft 26.x's.
 *
 * <p>Each input callback returns whether it used the input; unused input goes on to
 * {@link Screen}, which closes the screen on Escape.</p>
 */
public abstract class CompatScreen extends Screen {

    protected CompatScreen(String title) {
        super(Component.literal(title));
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

    /** @param key a GLFW key code, or {@code -1} for a key the screens do not use ({@link #KEYS}) */
    protected boolean key(int key, int modifiers) {
        return false;
    }

    /**
     * Sees every key press before {@link #key}, as Minecraft's version-independent key name
     * ({@code key.keyboard.g}), for binding keys.
     */
    protected boolean keyNamed(String name) {
        return false;
    }

    protected boolean typed(String text) {
        return false;
    }

    /**
     * Tells the game whether a text field of this screen has focus: the window sends typed
     * characters only while text input is on (always so under 26.3's SDL), and 26.2 switches
     * the input method with it.
     */
    protected void textInput(boolean focused) {
        Minecraft.getInstance().onTextInputFocusChange(this, focused);
    }

    /** @return the system clipboard's text, empty when it holds none */
    protected String clipboard() {
        String text = Minecraft.getInstance().keyboardHandler.getClipboard();
        return text == null ? "" : text;
    }

    @Override
    public void removed() {
        textInput(false);
        super.removed();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        draw(GfxCanvas.wrap(graphics), mouseX, mouseY, partialTick);
    }

    /** The screen draws its own backdrop; the vanilla one would blur the HUD behind it. */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return mouseDown(event.x(), event.y(), toGlfwButton(event.button())) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return mouseUp(event.x(), event.y(), toGlfwButton(event.button())) || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return mouseDrag(event.x(), event.y(), toGlfwButton(event.button()), dx, dy) || super.mouseDragged(event, dx, dy);
    }

    /**
     * @return the button as GLFW numbers it (left 0, right 1, middle 2), which the shared screens
     * use. 26.2 reads the mouse through GLFW, where the two agree; 26.3 through SDL, which counts
     * from 1 with middle before right, so its left click would otherwise arrive as a right click.
     * The {@code InputConstants} values are each version's own, compiled in.
     */
    private static int toGlfwButton(int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) return 0;
        if (button == InputConstants.MOUSE_BUTTON_RIGHT) return 1;
        if (button == InputConstants.MOUSE_BUTTON_MIDDLE) return 2;
        return button >= InputConstants.MOUSE_BUTTON_4 ? button - InputConstants.MOUSE_BUTTON_4 + 3 : -1;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        return scroll(x, y, scrollY) || super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return keyNamed(InputConstants.getKey(event).getName())
                || key(toGlfw(event.key()), toGlfwModifiers(event.modifiers())) || super.keyPressed(event);
    }

    /**
     * Key codes the shared screens act on, as {@code {InputConstants code, GLFW code}}. 26.2 reads
     * keys through GLFW, where the two are equal; 26.3 through SDL, whose codes differ and overlap
     * GLFW's (its Up arrow is GLFW's R), so every key {@link #key} sees is translated.
     */
    private static final int[][] KEYS = {
            {InputConstants.KEY_ESCAPE, 256},
            {InputConstants.KEY_RETURN, 257},
            {InputConstants.KEY_BACKSPACE, 259},
            {InputConstants.KEY_DELETE, 261},
            {InputConstants.KEY_RIGHT, 262},
            {InputConstants.KEY_LEFT, 263},
            {InputConstants.KEY_DOWN, 264},
            {InputConstants.KEY_UP, 265},
            {InputConstants.KEY_HOME, 268},
            {InputConstants.KEY_END, 269},
            {InputConstants.KEY_NUMPADENTER, 335},
            {InputConstants.KEY_R, 82},
            {InputConstants.KEY_V, 86},
    };

    /** @return the GLFW code of a key the screens use, or {@code -1} for any other key */
    private static int toGlfw(int key) {
        for (int[] pair : KEYS) {
            if (pair[0] == key) return pair[1];
        }
        return -1;
    }

    /** @return Shift, Control, Alt and Super as GLFW modifier bits */
    private static int toGlfwModifiers(int modifiers) {
        return ((modifiers & InputConstants.MOD_SHIFT) != 0 ? 0x1 : 0)
                | ((modifiers & InputConstants.MOD_CONTROL) != 0 ? 0x2 : 0)
                | ((modifiers & InputConstants.MOD_ALT) != 0 ? 0x4 : 0)
                | ((modifiers & InputConstants.MOD_SUPER) != 0 ? 0x8 : 0);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return typed(event.codepointAsString()) || super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
