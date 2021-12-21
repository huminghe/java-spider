package com.gs.spider.utils;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        add('\t');
        add('\f');
        add('\0');
    }};

    private static Set<Character> strongSentenceEndChars = new HashSet<Character>() {{
        add('\n');
        add('\r');
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
        add('\t');
        add('\f');
        add('\0');
    }};

    private static Set<Character> subSentenceEndChars = new HashSet<Character>() {{
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
        add('，');
        add(',');
    }};

    private static Set<Character> subSentenceEndNextChars = new HashSet<Character>() {{
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
        add('，');
        add(',');
    }};

    private static final Pattern NUM_PREFIX_PATTERN = Pattern.compile("(^[(（]?[0-9]+[)）.、]( )?)|(^[(（][一二三四五六七八九十]+[)）.、]( )?)");

    public static List<String> toSentences(String text, int maxLen) {
        List<String> sentences = toSentence(text);
        sentences = sentences.stream().flatMap(sentence -> {
            List<String> subSentences = new LinkedList<>();
            if (sentence.length() <= maxLen) {
                subSentences.add(sentence);
            } else {
                List<String> sentencesShorted = toSubSentence(sentence);
                subSentences.addAll(sentencesShorted);
            }
            return subSentences.stream();
        }).collect(Collectors.toList());
        List<String> results = new LinkedList<>();
        StringBuilder tmp = new StringBuilder();
        for (String sentence : sentences) {
            if (sentence.length() + tmp.length() >= maxLen) {
                if (tmp.length() > 0) {
                    String result = tmp.toString();
                    if (result.length() > maxLen) {
                        result = result.substring(0, maxLen);
                    }
                    results.add(result);
                    tmp.setLength(0);
                }
            }
            tmp.append(sentence);
        }
        if (tmp.length() > 0) {
            String result = tmp.toString();
            if (result.length() > maxLen) {
                result = result.substring(0, maxLen);
            }
            results.add(result);
        }
        return results;
    }

    public static List<String> toSubSentence(String text) {

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
            if (subSentenceEndChars.contains(c)) {
                cut = true;
                if (textLen > idx + 1 && subSentenceEndNextChars.contains(chars[idx + 1]) && !strongSentenceEndChars.contains(c)) {
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
                if (textLen > idx + 1 && sentenceEndNextChars.contains(chars[idx + 1]) && !strongSentenceEndChars.contains(c)) {
                    cut = false;
                    preCut = true;
                }
            }
            if (cut) {
                sentences.add(sb.toString().trim());
                sb.setLength(0);
            }
            idx = idx + 1;
        }
        if (sb.length() > 0) {
            sentences.add(sb.toString().trim());
        }
        return sentences;
    }

    public static String removeNumPrefix(String sentence) {
        return NUM_PREFIX_PATTERN.matcher(sentence).replaceFirst("");
    }

}
