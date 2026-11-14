package com.example.mathsolver;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MathSolver implements ClientModInitializer {

    // Ornek: "39 + altı = ?"  /  "12 x 4 = ?"
    private static final Pattern QUESTION = Pattern.compile(
        "(\\d+|\\p{L}+)\\s*([+\\-*/×÷]|\\s[xX]\\s)\\s*(\\d+|\\p{L}+)\\s*=\\s*\\?");

    private static final Map<String, Integer> WORDS = new HashMap<>();
    static {
        String[] ones = {"sıfır","bir","iki","üç","dört","beş","altı","yedi","sekiz","dokuz"};
        for (int i = 0; i < ones.length; i++) WORDS.put(ones[i], i);
        String[] tens = {"on","yirmi","otuz","kırk","elli","altmış","yetmiş","seksen","doksan"};
        for (int i = 0; i < tens.length; i++) WORDS.put(tens[i], (i + 1) * 10);
        WORDS.put("yüz", 100);
    }

    private String pending = null;
    private int delay = 0;
    private String lastQ = "";
    private long lastT = 0;
    private final Random rnd = new Random();

    @Override
    public void onInitializeClient() {
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> handle(msg.getString()));
        ClientReceiveMessageEvents.CHAT.register((msg, signed, sender, params, ts) -> handle(msg.getString()));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pending != null && --delay <= 0) {
                if (client.player != null) client.player.networkHandler.sendChatMessage(pending);
                pending = null;
            }
        });
    }

    private void handle(String text) {
        Matcher m = QUESTION.matcher(text);
        while (m.find()) {
            Integer a = parse(m.group(1));
            Integer b = parse(m.group(3));
            if (a == null || b == null) continue;

            String q = m.group(0);
            long now = System.currentTimeMillis();
            if (q.equals(lastQ) && now - lastT < 5000) return; // tekrar eden mesaj
            lastQ = q;
            lastT = now;

            String op = m.group(2).trim();
            double r;
            switch (op) {
                case "+": r = a + b; break;
                case "-": r = a - b; break;
                case "*": case "x": case "X": case "×": r = (double) a * b; break;
                case "/": case "÷": if (b == 0) return; r = (double) a / b; break;
                default: return;
            }
            pending = (r == Math.floor(r)) ? String.valueOf((long) r) : String.valueOf(r);
            delay = 20 + rnd.nextInt(30); // 1-2.5 sn bekle
            return;
        }
    }

    private Integer parse(String s) {
        s = s.trim();
        if (s.matches("\\d+")) return Integer.parseInt(s);
        return WORDS.get(s.toLowerCase(new Locale("tr", "TR")));
    }
}
