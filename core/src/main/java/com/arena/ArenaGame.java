package com.arena;

import com.arena.audio.AudioManager;
import com.arena.render.Assets;
import com.arena.screens.MenuScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.Box2D;

public class ArenaGame extends Game {
    public SpriteBatch batch;
    public Assets assets;
    public AudioManager audio;

    @Override
    public void create() {
        Box2D.init();
        batch = new SpriteBatch(4000);
        assets = new Assets();
        audio = new AudioManager();
        audio.startMusic();
        setScreen(new MenuScreen(this));
    }

    @Override
    public void dispose() {
        if (screen != null) screen.dispose();
        batch.dispose();
        assets.dispose();
        audio.dispose();
    }
}
