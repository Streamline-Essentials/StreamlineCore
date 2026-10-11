package net.streamline.client.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.streamline.client.Canvas;

/**
 * A {@link Screen} whose drawing and input arrive through version-agnostic callbacks, so
 * Streamline's screens are written once. This is Minecraft 1.21.1's.
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

    /** @param key a GLFW key code */
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
     * Tells the game whether a text field of this screen has focus. Only 26.x asks: its SDL
     * window sends typed characters only while text input is on.
     */
    protected void textInput(boolean focused) {
    }

    /** @return the system clipboard's text, empty when it holds none */
    protected String clipboard() {
        String text = Minecraft.getInstance().keyboardHandler.getClipboard();
        return text == null ? "" : text;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        draw(GfxCanvas.wrap(graphics), mouseX, mouseY, partialTick);
    }

    /** The screen draws its own backdrop; the vanilla one would blur the HUD behind it. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        return mouseDown(x, y, button) || super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        return mouseUp(x, y, button) || super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return mouseDrag(x, y, button, dx, dy) || super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        return scroll(x, y, scrollY) || super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        return keyNamed(InputConstants.getKey(key, scancode).getName()) || key(key, modifiers) || super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return typed(String.valueOf(character)) || super.charTyped(character, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
