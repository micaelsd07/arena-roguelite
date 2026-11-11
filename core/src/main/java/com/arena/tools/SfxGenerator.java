package com.arena.tools;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.Random;

/**
 * Sintetizador chiptune (estilo sfxr) que gera todos os efeitos e a musica em .wav 16-bit mono.
 * Uso: ./gradlew :lwjgl3:generateAudio   (ou java SfxGenerator &lt;pastaDeSaida&gt;)
 */
public final class SfxGenerator {
    static final int SR = 22050;
    enum Wave { SQUARE, TRIANGLE, SAW, NOISE }

    private static final Random RNG = new Random(1234);

    public static void main(String[] args) throws IOException {
        File out = new File(args.length > 0 ? args[0] : "assets/sfx");
        Files.createDirectories(out.toPath());

        write(out, "shoot",        tone(0.09f, 900, 380, Wave.SQUARE, 0.5f, 0.125f));
        write(out, "enemy_shoot",  tone(0.16f, 320, 160, Wave.SAW, 0.5f, 0.5f));
        write(out, "hit",          mix(tone(0.05f, 260, 140, Wave.SQUARE, 0.4f, 0.5f), noise(0.05f, 2, 0.5f)));
        write(out, "death_chaser", mix(noise(0.20f, 2, 0.7f), tone(0.15f, 400, 80, Wave.SQUARE, 0.35f, 0.5f)));
        write(out, "death_shooter",mix(tone(0.28f, 700, 90, Wave.SAW, 0.45f, 0.5f), noise(0.22f, 3, 0.45f)));
        write(out, "death_tank",   mix(noise(0.45f, 6, 0.8f), tone(0.45f, 140, 35, Wave.SQUARE, 0.55f, 0.5f)));
        write(out, "player_hurt",  vibrato(tone(0.28f, 260, 110, Wave.SQUARE, 0.6f, 0.5f), 18f));
        write(out, "pickup",       tone(0.07f, 1300, 1900, Wave.SQUARE, 0.35f, 0.25f));
        write(out, "ricochet",     tone(0.06f, 1700, 900, Wave.TRIANGLE, 0.5f, 0.5f));
        write(out, "select",       concat(tone(0.06f, 660, 660, Wave.SQUARE, 0.4f, 0.25f),
                                          tone(0.10f, 990, 990, Wave.SQUARE, 0.4f, 0.25f)));
        write(out, "level_up",     arpeggio(new float[]{523, 659, 784, 1047, 1319}, 0.07f, Wave.SQUARE, 0.4f));
        write(out, "wave_start",   arpeggio(new float[]{392, 392, 523}, 0.12f, Wave.SAW, 0.4f));
        write(out, "game_over",    arpeggio(new float[]{392, 330, 262, 196, 131}, 0.20f, Wave.TRIANGLE, 0.6f));
        write(out, "music",        music());
        System.out.println("Audio gerado em " + out.getAbsolutePath());
    }

    // ------------------------------------------------------------------ geradores

    /** Tom com varredura exponencial de frequencia e envelope ataque/decay. */
    static float[] tone(float dur, float f0, float f1, Wave wave, float vol, float duty) {
        int n = (int) (dur * SR);
        float[] s = new float[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            double f = f0 * Math.pow(f1 / f0, t);
            phase += f / SR;
            s[i] = osc(wave, phase, duty) * vol * env(i, n, 0.005f);
        }
        return s;
    }

    /** Ruido "sample and hold": hold maior = som mais grave; aumenta ao longo do tempo (queda). */
    static float[] noise(float dur, int hold, float vol) {
        int n = (int) (dur * SR);
        float[] s = new float[n];
        float v = 0;
        int counter = 0;
        for (int i = 0; i < n; i++) {
            int h = hold + (int) (hold * 3f * i / n);
            if (counter-- <= 0) { v = RNG.nextFloat() * 2f - 1f; counter = h; }
            s[i] = v * vol * env(i, n, 0.002f);
        }
        return s;
    }

    static float osc(Wave w, double phase, float duty) {
        double p = phase - Math.floor(phase);
        switch (w) {
            case SQUARE: return p < duty ? 1f : -1f;
            case TRIANGLE: return (float) (p < 0.5 ? 4 * p - 1 : 3 - 4 * p);
            case SAW: return (float) (2 * p - 1);
            default: return RNG.nextFloat() * 2f - 1f;
        }
    }

    static float env(int i, int n, float attackSec) {
        int a = Math.max(1, (int) (attackSec * SR));
        float attack = Math.min(1f, i / (float) a);
        float decay = 1f - i / (float) n;
        return attack * decay * decay;
    }

    static float[] vibrato(float[] s, float hz) {
        for (int i = 0; i < s.length; i++) s[i] *= 0.65f + 0.35f * (float) Math.sin(2 * Math.PI * hz * i / SR);
        return s;
    }

    static float[] arpeggio(float[] notes, float noteDur, Wave w, float vol) {
        float[] out = new float[0];
        for (float f : notes) out = concat(out, tone(noteDur, f, f * 0.99f, w, vol, 0.5f));
        return out;
    }

    static float[] mix(float[] a, float[] b) {
        float[] o = new float[Math.max(a.length, b.length)];
        for (int i = 0; i < o.length; i++) o[i] = (i < a.length ? a[i] : 0) + (i < b.length ? b[i] : 0);
        return o;
    }

    static float[] concat(float[] a, float[] b) {
        float[] o = new float[a.length + b.length];
        System.arraycopy(a, 0, o, 0, a.length);
        System.arraycopy(b, 0, o, a.length, b.length);
        return o;
    }

    // ------------------------------------------------------------------ musica

    /** Loop de 8 compassos a 128 BPM em La menor: baixo (quadrada), arpejo (triangular) e chimbal (ruido). */
    static float[] music() {
        final float bpm = 128f;
        final int stepLen = (int) (SR * 60f / bpm / 4f); // semicolcheia
        // Progressao Am - F - C - G (2x), com variacao no final
        int[][] chords = {
                {57, 60, 64}, {53, 57, 60}, {48, 52, 55}, {55, 59, 62},
                {57, 60, 64}, {53, 57, 60}, {52, 55, 59}, {52, 56, 59}};
        int steps = chords.length * 16;
        float[] out = new float[steps * stepLen];
        double bassPhase = 0, leadPhase = 0;
        int[] arp = {0, 1, 2, 1, 0, 2, 1, 2};
        Random hatRng = new Random(7);

        for (int step = 0; step < steps; step++) {
            int[] chord = chords[step / 16];
            int inBar = step % 16;
            float bassF = midi(chord[0] - 12);
            boolean bassOn = inBar % 2 == 0;
            float leadF = midi(chord[arp[inBar % 8]] + 12);
            boolean kick = inBar % 4 == 0;
            boolean hat = inBar % 2 == 1;

            for (int j = 0; j < stepLen; j++) {
                int idx = step * stepLen + j;
                float t = j / (float) stepLen;
                float v = 0;
                if (bassOn) {
                    bassPhase += bassF / SR;
                    v += osc(Wave.SQUARE, bassPhase, 0.5f) * 0.16f * (1f - t * 0.5f);
                }
                leadPhase += leadF / SR;
                v += osc(Wave.TRIANGLE, leadPhase, 0.5f) * 0.14f * (1f - t) * (1f - t);
                if (kick && t < 0.6f) {
                    double kf = 110 * Math.pow(0.25, t / 0.6);
                    v += (float) Math.sin(2 * Math.PI * kf * j / SR) * 0.30f * (1f - t / 0.6f);
                }
                if (hat && t < 0.25f) v += (hatRng.nextFloat() * 2f - 1f) * 0.05f * (1f - t / 0.25f);
                out[idx] = v;
            }
        }
        return out;
    }

    static float midi(int note) { return (float) (440.0 * Math.pow(2, (note - 69) / 12.0)); }

    // ------------------------------------------------------------------ WAV

    static void write(File dir, String name, float[] samples) throws IOException {
        ByteBuffer pcm = ByteBuffer.allocate(samples.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (float s : samples) pcm.putShort((short) (Math.max(-1f, Math.min(1f, s)) * 32000));
        int dataLen = pcm.capacity();
        ByteBuffer h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        h.put("RIFF".getBytes()).putInt(36 + dataLen).put("WAVE".getBytes());
        h.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1)
                .putInt(SR).putInt(SR * 2).putShort((short) 2).putShort((short) 16);
        h.put("data".getBytes()).putInt(dataLen);
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(new File(dir, name + ".wav")))) {
            os.write(h.array());
            os.write(pcm.array());
        }
    }

    private SfxGenerator() {}
}
