package com.gs.spider.utils;

import com.gs.spider.model.utils.KeywordRaw;
import com.gs.spider.model.utils.KeywordResult;
import com.gs.spider.model.utils.MatchHit;
import com.gs.spider.model.utils.Word;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.Viterbi.ViterbiSegment;
import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author huminghe
 * @date 2021/7/19
 */
@Component
public class KeywordExtractor implements NLPExtractor {

    private Logger LOG = LoggerFactory.getLogger(getClass());

    private static Segment hanlpSegment = new ViterbiSegment().enableOffset(true).enablePlaceRecognize(true);

    private static Set<String> NOUN = new HashSet<String>() {{
        add("n");
        add("an");
        add("nr");
        add("ns");
        add("nt");
        add("nz");
        add("cb");
    }};
    private static Set<String> VERB = new HashSet<String>() {{
        add("v");
        add("vn");
    }};
    private static float DEFAULT_IDF = 8.0F;

    private List<Pair<Float, Float>> similarFilterList = new LinkedList<>();

    {
        similarFilterList.add(new Pair<>(0.0F, 0.5F));
        similarFilterList.add(new Pair<>(0.3F, 0.4F));
        similarFilterList.add(new Pair<>(0.4F, 0.3F));
        similarFilterList.add(new Pair<>(0.5F, 0.2F));
        similarFilterList.add(new Pair<>(0.7F, 0.1F));
        similarFilterList.add(new Pair<>(0.8F, 0.0F));
    }

    private Set<String> allStopwords;
    private Set<String> filterWords;
    private Set<String> feedWords;
    private Set<String> phrases;
    private AhoCorasickMatcher<Boolean> matcher;

    private Map<String, Float> mixIdf;
    private Map<String, float[]> wordVecs;

    {
        allStopwords = Loader.load("/data/all.stopwords");
        filterWords = Loader.load("/data/filter.dict");
        feedWords = Loader.load("/data/feed_keyword.dict");
        phrases = Loader.load("/data/phrase.dict");
        Set<String> entities = Loader.load("/data/entity_names.txt");
        phrases.addAll(feedWords);
        phrases.addAll(entities);
        matcher = new AhoCorasickMatcher<>(phrases.stream().filter(x -> x.length() >= 2).collect(Collectors.toMap(x -> x, x -> true)));
        mixIdf = Loader.loadIdf("/data/zword.iwf");
        String vecsPath = System.getenv("KEYWORD_VECS_PATH");
        if (StringUtils.isBlank(vecsPath)) {
            vecsPath = "/Users/huminghe/Documents/tmp/0721/word_vecs_tencent.txt";
        }
        wordVecs = Loader.loadWordVecs(vecsPath, 200);
    }

    @Override
    public Map<String, Set<String>> extractNamedEntity(String content) {
        return null;
    }

    @Override
    public List<String> extractSummary(String content) {
        return null;
    }

    @Override
    public List<String> extractKeywords(String content) {
        return extractKeywords("", content);
    }

    @Override
    public List<String> extractKeywords(String title, String content) {
        List<KeywordResult> keywords = extractKeywords(title, content, 6, 0.25);
        return keywords.stream().map(KeywordResult::getWord).collect(Collectors.toList());
    }

    public List<KeywordResult> extractKeywords(String title, String content, int topK, double fraction) {

        List<KeywordResult> result;

        int nKeywords = Math.max(topK, 20);
        int nForEmbed = 10;
        List<String> titleWords = computeBaseKeywords(title, "", nKeywords).stream().map(KeywordResult::getWord).collect(Collectors.toList());
        List<KeywordResult> baseKeywords = computeBaseKeywords(title, content, nKeywords);
        List<String> words = baseKeywords.stream().map(KeywordResult::getWord).limit(nForEmbed).collect(Collectors.toList());

        if (!words.isEmpty() && !wordVecs.isEmpty()) {
            float[] titleEmbedding = Distance.wordsToVector(titleWords, wordVecs);
            float[] docEmbedding = Distance.wordsToVector(words, wordVecs);

            result = baseKeywords.stream()
                .filter(keyword -> keyword.isInTitle() || keyword.getFrequency() >= 2)
                .map(keyword -> {
                    boolean similarityFilter = false;
                    if (wordVecs.containsKey(keyword.getWord())) {
                        float titleSimilarity = Distance.cosine(titleEmbedding, wordVecs.get(keyword.getWord()));
                        float contentSimilarity = Distance.cosine(docEmbedding, wordVecs.get(keyword.getWord()));

                        similarityFilter = similarFilterList.stream()
                            .map(pair -> titleSimilarity >= pair.getKey() && contentSimilarity >= pair.getValue())
                            .reduce((a, b) -> a || b).orElse(false);
                        keyword.setSimilarity((titleSimilarity + contentSimilarity) / 2.0F);
                    }
                    if ((keyword.isInTitle() && keyword.getWeight() > 0.12) || similarityFilter) {
                        return keyword;
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted((k1, k2) -> {
                    if (k1.isInTitle() && !k2.isInTitle()) {
                        return -1;
                    } else if (!k1.isInTitle() && k2.isInTitle()) {
                        return 1;
                    } else {
                        return -Float.compare(k1.getSimilarity(), k2.getSimilarity());
                    }
                })
                .collect(Collectors.toList());
        } else {
            result = baseKeywords;
        }

        if (fraction > 0.0 && fraction < 1.0) {
            double maxWeight = result.stream()
                .map(KeywordResult::getWeight)
                .max(Double::compare).orElse(0.0);
            double weightLimit = maxWeight * fraction;
            result = result.stream().filter(k -> k.getWeight() > weightLimit).collect(Collectors.toList());
        }

        if (topK > 0) {
            result = result.stream().limit(topK).collect(Collectors.toList());
        }
        return result;
    }

    private List<KeywordResult> computeBaseKeywords(String title, String content, int topK) {
        Set<String> titleWordSet = candidateWords(title)
            .stream().map(Word::getWord)
            .filter(word -> !filterByDict(word))
            .collect(Collectors.toSet());

        List<Word> allWords = candidateWords(title + " " + content);

        Map<String, Integer> wordFreq = allWords.stream().map(Word::getWord)
            .collect(Collectors.groupingBy(x -> x))
            .entrySet()
            .stream().map(x -> {
                String word = x.getKey();
                int cnt = x.getValue().size();
                return new Pair<>(word, cnt);
            })
            .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        int tfSum = allWords.size();

        List<KeywordResult> sortedKeywordsList = allWords.stream()
            .filter(wordObj -> phrases.contains(wordObj.getWord()) && !filterByDict(wordObj.getWord()))
            .map(wordObj -> {
                double tf = wordFreq.getOrDefault(wordObj.getWord(), 0) * 1.0 / (tfSum + 1.0);
                double idf = mixIdf.getOrDefault(wordObj.getWord(), DEFAULT_IDF);
                double tfidf = tf * idf;

                double titleScore = 0;
                if (titleWordSet.contains(wordObj.getWord()) && wordObj.getLen() > 0) {
                    if (NOUN.contains(wordObj.getPosTag())) {
                        titleScore = Math.min(3, wordObj.getLen() - 1) * 0.2;
                    } else {
                        titleScore = Math.min(4.5, wordObj.getLen() - 1) * 0.1;
                    }
                }

                double lenScore = wordObj.getLen() * 1.0 / (wordObj.getLen() + 4);

                double speechScore = 0;
                if (NOUN.contains(wordObj.getPosTag())) {
                    speechScore = 0.6;
                } else if (VERB.contains(wordObj.getPosTag())) {
                    speechScore = 0.2;
                }

                double weight = tfidf * 0.7 + titleScore * 0.2 + lenScore * 0.05 + speechScore * 0.05;

                return new KeywordRaw(wordObj.getWord(), weight);
            })
            .collect(Collectors.groupingBy(KeywordRaw::getWord))
            .entrySet()
            .stream()
            .map(info -> {
                String word = info.getKey();
                double maxWeight = info.getValue().stream().map(KeywordRaw::getWeight).max(Double::compare).orElse(0.0);
                return new KeywordRaw(word, maxWeight);
            })
            .sorted((a, b) -> Double.compare(b.getWeight(), a.getWeight()))
            .limit(topK)
            .map(keyword -> {
                boolean inTitle = titleWordSet.contains(keyword.getWord());
                int frequency = wordFreq.getOrDefault(keyword.getWord(), 0);
                return new KeywordResult(keyword.getWord(), keyword.getWeight(), frequency, inTitle, 0.0F);
            })
            .collect(Collectors.toList());

        return sortedKeywordsList;
    }

    private Boolean filterByDict(String word) {
        return allStopwords.contains(word) || filterWords.contains(word) || StringUtils.isNumeric(word);
    }

    private List<Word> candidateWords(String content) {
        List<Word> words = hanlpSegment.seg(content)
            .stream()
            .map(term -> new Word(term.word, term.offset, term.length(), term.nature.toString()))
            .filter(word -> StringUtils.isNotBlank(word.getWord()))
            .collect(Collectors.toList());

        Set<Integer> beginSet = words.stream().map(Word::getPosBegin).collect(Collectors.toSet());
        Set<Integer> endSet = words.stream().map(word -> word.getPosBegin() + word.getLen()).collect(Collectors.toSet());

        Map<Integer, Integer> matcherMap = new HashMap<>();

        matcher.matching(content, false)
            .stream()
            .filter(matchHit -> beginSet.contains(matchHit.getBegin()) && endSet.contains(matchHit.getEnd()))
            .collect(Collectors.groupingBy(MatchHit::getEnd))
            .forEach((key, value) -> {
                int end = key;
                int start = value.stream().map(MatchHit::getBegin).min(Integer::compare).orElse(0);
                matcherMap.put(end, start);
            });

        List<Word> candidateWords = new LinkedList<>();
        int combineBegin = content.length();
        int wordsLen = words.size();
        for (int ind = wordsLen - 1; ind >= 0; ind--) {
            Word wordObj = words.get(ind);
            int end = wordObj.getPosBegin() + wordObj.getLen();
            int matcherBegin = matcherMap.getOrDefault(end, wordObj.getPosBegin());
            if (matcherBegin < wordObj.getPosBegin() && combineBegin > wordObj.getPosBegin()) {
                String wordString = content.substring(matcherBegin, end);
                Word word = new Word(wordString, matcherBegin, end - matcherBegin, "cb");
                candidateWords.add(word);
                combineBegin = matcherBegin;
            } else if (combineBegin > wordObj.getPosBegin()) {
                candidateWords.add(wordObj);
            }
        }
        return candidateWords;
    }

}