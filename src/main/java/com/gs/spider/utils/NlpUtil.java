package com.gs.spider.utils;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author huminghe
 * @date 2021/9/10
 */
public class NlpUtil {

    private static Set<Character> sentenceEndChars = new HashSet<Character>() {{
        add('。');
        add('？');
        add('！');
        add('!');
        add('?');
        add(';');
        add('；');
        add('\n');
        add('\r');
        add('\b');
        add(' ');
        add('\u00A0');
    }};

    private static Set<Character> sentenceEndNextChars = new HashSet<Character>() {{
        add('。');
        add('？');
        add('！');
        add('!');
        add('?');
        add(';');
        add('；');
        add('\n');
        add('\r');
        add('\b');
        add('.');
        add('\"');
        add('\'');
        add('”');
        add('“');
        add('‘');
        add('’');
        add(' ');
        add('\u00A0');
    }};

    public static List<String> toSentence(String text) {

        List<String> sentences = new LinkedList<>();
        StringBuilder sb = new StringBuilder();
        boolean preCut = false;
        int textLen = text.length();

        char[] chars = text.toCharArray();
        int idx = 0;
        for (char c : chars) {
            sb.append(c);
            boolean cut = false;
            if (preCut) {
                cut = true;
                preCut = false;
            }
            if (sentenceEndChars.contains(c)) {
                cut = true;
                if (textLen > idx + 1 && sentenceEndNextChars.contains(chars[idx + 1])) {
                    cut = false;
                    preCut = true;
                }
            }
            if (cut) {
                sentences.add(sb.toString());
                sb.setLength(0);
            }
            idx = idx + 1;
        }
        if (sb.length() > 0) {
            sentences.add(sb.toString());
        }
        return sentences;
    }

}
