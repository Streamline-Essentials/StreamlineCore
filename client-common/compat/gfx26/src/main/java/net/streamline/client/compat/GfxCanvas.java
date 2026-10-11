package net.streamline.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.streamline.client.Canvas;
import net.streamline.client.ModernText;

/**
 * {@link Canvas} over Minecraft 26.x's {@code GuiGraphicsExtractor}, whose transform is a JOML {@code Matrix3x2fStack}.
 */
public final class GfxCanvas implements Canvas {

    private final GuiGraphicsExtractor graphics;
    private final Font font;

    private GfxCanvas(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
        this.font = Minecraft.getInstance().font;
    }

    public static GfxCanvas wrap(GuiGraphicsExtractor graphics) {
        return new GfxCanvas(graphics);
    }

    @Override
    public int width() {
        return graphics.guiWidth();
    }

    @Override
    public int height() {
        return graphics.guiHeight();
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y2, color);
    }

    @Override
    public void text(String text, int x, int y, int color, boolean shadow) {
        graphics.text(font, ModernText.parse(text), x, y, color, shadow);
    }

    @Override
    public int textWidth(String text) {
        return font.width(ModernText.parse(text));
    }

    @Override
    public int lineHeight() {
        return font.lineHeight;
    }

    @Override
    public void push() {
        graphics.pose().pushMatrix();
    }

    @Override
    public void pop() {
        graphics.pose().popMatrix();
    }

    @Override
    public void translate(float x, float y) {
        graphics.pose().translate(x, y);
    }

    @Override
    public void scale(float factor) {
        graphics.pose().scale(factor, factor);
    }

    @Override
    public void clip(int x1, int y1, int x2, int y2) {
        graphics.enableScissor(x1, y1, x2, y2);
    }

    @Override
    public void endClip() {
        graphics.disableScissor();
    }
}
