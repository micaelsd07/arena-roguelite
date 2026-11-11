package com.arena.render;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Disposable;

/** Texturas geradas por codigo (Pixmap). Tudo e branco e tingido pelo SpriteBatch -> um unico batch. */
public class Assets implements Disposable {
    public final Texture circle;
    public final Texture ring;
    public final Texture pixel;
    public final BitmapFont font;

    public Assets() {
        circle = makeCircle(64, false);
        ring = makeCircle(128, true);
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(1, 1, 1, 1);
        p.fill();
        pixel = new Texture(p);
        p.dispose();
        font = new BitmapFont();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        font.setUseIntegerPositions(false);
    }

    private static Texture makeCircle(int size, boolean ringOnly) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        float c = (size - 1) / 2f, r = size / 2f - 1f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float d = (float) Math.hypot(x - c, y - c);
                float a = Math.max(0f, Math.min(1f, r - d + 0.5f)); // borda suavizada
                if (ringOnly) a *= Math.max(0f, Math.min(1f, d - (r - 3f)));
                // leve sombreado radial para dar volume
                float shade = ringOnly ? 1f : 1f - 0.25f * (d / r);
                p.drawPixel(x, y, rgba(shade, shade, shade, a));
            }
        }
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return t;
    }

    private static int rgba(float r, float g, float b, float a) {
        return ((int) (r * 255) << 24) | ((int) (g * 255) << 16) | ((int) (b * 255) << 8) | (int) (a * 255);
    }

    @Override
    public void dispose() {
        circle.dispose();
        ring.dispose();
        pixel.dispose();
        font.dispose();
    }
}
