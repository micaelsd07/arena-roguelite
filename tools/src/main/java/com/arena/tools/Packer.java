package com.arena.tools;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Empacotador simples (prateleiras) que grava um TextureAtlas no formato do LibGDX.
 * Cada regiao ganha 2 px de borda extrudada para evitar "sangramento" com filtro linear.
 */
final class Packer {
    static final class Entry {
        final String name; final int index; final BufferedImage img;
        int x, y;
        Entry(String name, int index, BufferedImage img) { this.name = name; this.index = index; this.img = img; }
    }

    private static final int PAD = 2;
    private final List<Entry> entries = new ArrayList<>();

    void add(String name, BufferedImage img) { add(name, -1, img); }

    void add(String name, int index, BufferedImage img) { entries.add(new Entry(name, index, img)); }

    void write(File dir, String baseName) throws IOException {
        dir.mkdirs();
        List<Entry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingInt((Entry e) -> -e.img.getHeight()).thenComparing(e -> e.name));
        int size = 512;
        while (!tryPack(sorted, size)) size *= 2;
        BufferedImage page = Img.argb(size, size);
        for (Entry e : sorted) blitExtruded(page, e);
        ImageIO.write(page, "png", new File(dir, baseName + ".png"));
        try (PrintWriter w = new PrintWriter(new File(dir, baseName + ".atlas"), "UTF-8")) {
            w.println(baseName + ".png");
            w.println("size:" + size + "," + size);
            w.println("format:RGBA8888");
            w.println("filter:Linear,Linear");
            w.println("repeat:none");
            for (Entry e : entries) {
                w.println(e.name);
                w.println("bounds:" + e.x + "," + e.y + "," + e.img.getWidth() + "," + e.img.getHeight());
                if (e.index >= 0) w.println("index:" + e.index);
            }
        }
        System.out.println("Atlas " + size + "x" + size + " com " + entries.size() + " regioes");
    }

    private static boolean tryPack(List<Entry> list, int size) {
        int x = 0, y = 0, rowH = 0;
        for (Entry e : list) {
            int w = e.img.getWidth() + PAD * 2, h = e.img.getHeight() + PAD * 2;
            if (x + w > size) { x = 0; y += rowH; rowH = 0; }
            if (y + h > size || w > size) return false;
            e.x = x + PAD; e.y = y + PAD;
            x += w; rowH = Math.max(rowH, h);
        }
        return true;
    }

    private static void blitExtruded(BufferedImage page, Entry e) {
        int w = e.img.getWidth(), h = e.img.getHeight();
        for (int j = -PAD; j < h + PAD; j++)
            for (int i = -PAD; i < w + PAD; i++) {
                int sx = Math.max(0, Math.min(w - 1, i)), sy = Math.max(0, Math.min(h - 1, j));
                page.setRGB(e.x + i, e.y + j, e.img.getRGB(sx, sy));
            }
    }
}
