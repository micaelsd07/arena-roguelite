package com.arena.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.FloatArray;

/**
 * Iluminacao 2D: um FrameBuffer (meia resolucao) e limpo com a cor ambiente do bioma, recebe as luzes
 * em modo aditivo e depois e multiplicado sobre a cena (GL_DST_COLOR, GL_ZERO).
 */
public class Lighting implements Disposable {
    private FrameBuffer fbo;
    private TextureRegion fboRegion;
    private final FloatArray lights = new FloatArray(1024); // x, y, raio, r, g, b
    private final Matrix4 screenMatrix = new Matrix4();
    public float ambR = 0.2f, ambG = 0.2f, ambB = 0.25f;

    public void resize(int w, int h) {
        if (fbo != null) fbo.dispose();
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, Math.max(1, w / 2), Math.max(1, h / 2), false);
        fbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        fboRegion = new TextureRegion(fbo.getColorBufferTexture());
        fboRegion.flip(false, true);
    }

    public void clear() { lights.clear(); }

    public void add(float x, float y, float radius, float r, float g, float b) {
        lights.add(x, y, radius);
        lights.add(r, g, b);
    }

    public int count() { return lights.size / 6; }

    /** Desenha o mapa de luz e o aplica sobre o que ja esta na tela (dentro do viewport atual). */
    public void apply(SpriteBatch batch, Camera camera, TextureRegion lightTex, int vx, int vy, int vw, int vh) {
        if (fbo == null) return;
        fbo.begin();
        Gdx.gl.glClearColor(ambR, ambG, ambB, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < lights.size; i += 6) {
            float x = lights.get(i), y = lights.get(i + 1), rad = lights.get(i + 2);
            batch.setColor(lights.get(i + 3), lights.get(i + 4), lights.get(i + 5), 1f);
            batch.draw(lightTex, x - rad, y - rad, rad * 2, rad * 2);
        }
        batch.end();
        fbo.end(vx, vy, vw, vh);

        screenMatrix.setToOrtho2D(0, 0, 1, 1);
        batch.setProjectionMatrix(screenMatrix);
        batch.setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_ZERO);
        batch.setColor(1, 1, 1, 1);
        batch.begin();
        batch.draw(fboRegion, 0, 0, 1, 1);
        batch.end();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void dispose() { if (fbo != null) fbo.dispose(); }
}
