package com.arena.world;

/** Entrada ja traduzida para o mundo (preenchida pela tela, ou por um bot nos testes). */
public class InputState {
    public float moveX, moveY;   // -1..1
    public float aimX, aimY;     // ponto mirado, em metros
    public boolean firing;
    public boolean dash;         // pressionado neste frame
    public boolean special;      // pressionado neste frame
}
