package com.arena.audio;

/** Saida de audio usada pela logica do jogo. Permite rodar a simulacao sem som (testes). */
public interface GameAudio {
    void play(Sfx sfx);

    GameAudio SILENT = sfx -> { };
}
