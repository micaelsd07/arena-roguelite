package com.arena.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;

/** Atlas gerado pelo tools/ArtBuilder + fonte + shader de "flash" branco. */
public class Assets implements Disposable {
    public static final float PPM = 32f; // pixels do atlas por metro

    public final TextureAtlas atlas;
    public final BitmapFont font;
    public final ShaderProgram flashShader;
    public final TextureRegion pixel, soft, light, ring, shadow;
    private final ObjectMap<String, Array<TextureRegion>> cache = new ObjectMap<>();

    public Assets() {
        atlas = new TextureAtlas(Gdx.files.internal("atlas/game.atlas"));
        pixel = region("pixel");
        soft = region("soft");
        light = region("light");
        ring = region("ring");
        shadow = region("shadow");
        font = new BitmapFont();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        font.setUseIntegerPositions(false);

        String vert = "attribute vec4 a_position; attribute vec4 a_color; attribute vec2 a_texCoord0;\n"
                + "uniform mat4 u_projTrans; varying vec4 v_color; varying vec2 v_uv;\n"
                + "void main(){ v_color = a_color; v_color.a *= 255.0/254.0; v_uv = a_texCoord0; gl_Position = u_projTrans * a_position; }";
        String frag = "#ifdef GL_ES\nprecision mediump float;\n#endif\n"
                + "varying vec4 v_color; varying vec2 v_uv; uniform sampler2D u_texture;\n"
                + "void main(){ vec4 t = texture2D(u_texture, v_uv); gl_FragColor = vec4(v_color.rgb, t.a * v_color.a); }";
        flashShader = new ShaderProgram(vert, frag);
        if (!flashShader.isCompiled()) throw new GdxRuntimeException(flashShader.getLog());
    }

    public TextureRegion region(String name) {
        TextureRegion r = atlas.findRegion(name);
        if (r == null) r = atlas.findRegion(name, 0);
        if (r == null) throw new GdxRuntimeException("Regiao ausente no atlas: " + name);
        return r;
    }

    /** Todos os frames/variantes de uma regiao (ordenados pelo indice). */
    public Array<TextureRegion> frames(String name) {
        Array<TextureRegion> a = cache.get(name);
        if (a == null) {
            a = new Array<>();
            for (TextureAtlas.AtlasRegion r : atlas.findRegions(name)) a.add(r);
            if (a.isEmpty()) a.add(region(name));
            cache.put(name, a);
        }
        return a;
    }

    public TextureRegion frame(String name, float time, float fps) {
        Array<TextureRegion> a = frames(name);
        return a.get(((int) (time * fps)) % a.size);
    }

    @Override
    public void dispose() {
        atlas.dispose();
        font.dispose();
        flashShader.dispose();
    }
}
