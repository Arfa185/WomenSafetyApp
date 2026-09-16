package com.example.womensafety.util;

import java.util.Random;

/**
 * Generates short, human-friendly join codes (e.g. "K7QX2R") used to let
 * trusted contacts join a private tracking group without exposing the
 * underlying Firebase group key.
 */
public class CodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I to avoid confusion
    private static final int LENGTH = 6;

    public static String generate() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
