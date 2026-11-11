package com.arena.screens;

import com.arena.audio.AudioManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

final class AudioKeys {
    static void handle(AudioManager audio) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) audio.toggleMute();
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_SUBTRACT))
            audio.changeVolume(-0.1f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.EQUALS) || Gdx.input.isKeyJustPressed(Input.Keys.PLUS)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ADD))
            audio.changeVolume(0.1f);
    }

    private AudioKeys() {}
}
