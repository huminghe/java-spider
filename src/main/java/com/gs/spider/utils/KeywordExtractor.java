package com.gs.spider.utils;

import com.gs.spider.model.utils.KeywordRaw;
import com.gs.spider.model.utils.KeywordResult;
import com.gs.spider.model.utils.MatchHit;
import com.gs.spider.model.utils.Sentence;
import com.gs.spider.model.utils.Word;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.Viterbi.ViterbiSegment;
import com.hankcs.hanlp.seg.common.Term;
import org.javatuples.Pair;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
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

    public static final Segment hanlpSegment = new ViterbiSegment().enableOffset(true).enablePlaceRecognize(true);

    private static final Set<String> NOUN = new HashSet<String>() {{
        add("n");
        add("an");
        add("nr");
        add("ns");
        add("nt");
        add("nz");
        add("cb");
    }};
    private static final Set<String> IMPORTANT_NOUN = new HashSet<String>() {{
        add("nr");
        add("cb");
    }};
    private static final Set<String> VERB = new HashSet<String>() {{
        add("v");
        add("vn");
    }};
    private static final float DEFAULT_IDF = 6.0F;

    private final List<Pair<Float, Float>> similarFilterList = new LinkedList<>();

    {
        similarFilterList.add(new Pair<>(0.0F, 0.5F));
        similarFilterList.add(new Pair<>(0.3F, 0.4F));
        similarFilterList.add(new Pair<>(0.4F, 0.3F));
        similarFilterList.add(new Pair<>(0.5F, 0.2F));
        similarFilterList.add(new Pair<>(0.7F, 0.1F));
        similarFilterList.add(new Pair<>(0.8F, 0.0F));
    }

    private final Set<String> allStopwords;
    private final Set<String> filterWords;
    private final Set<String> phrases;
    private final Set<String> gridKeywords;
    private final AhoCorasickMatcher<Boolean> matcher;

    private final Map<String, Float> mixIdf;
    private final Map<String, float[]> wordVecs;

    private final AhoCorasickMatcher<Boolean> summaryMatcher;

    {
        allStopwords = Loader.load("/data/all.stopwords");
        filterWords = Loader.load("/data/filter.dict");
        Set<String> feedWords = Loader.load("/data/feed_keyword.dict");
        phrases = Loader.load("/data/phrase.dict");
        gridKeywords = Loader.load("/data/grid_keyword.txt");
        Set<String> entities = Loader.load("/data/entity_names.txt", 1);
        phrases.addAll(feedWords);
        phrases.addAll(entities);
        phrases.addAll(gridKeywords);
        matcher = new AhoCorasickMatcher<>(phrases.stream().filter(x -> x.length() >= 2).collect(Collectors.toMap(x -> x, x -> true)));
        mixIdf = Loader.loadIdf("/data/zhihu_core_coarse.dict");
        wordVecs = Loader.loadWordVecs(StaticValue.wordVectorsPath, 200);

        Set<String> summaryWords = Loader.load("/data/summary_words.txt");
        summaryMatcher = new AhoCorasickMatcher<>(summaryWords.stream().collect(Collectors.toMap(x -> x, x -> true)));
    }

    @Override
    public Map<String, Set<String>> extractNamedEntity(String content) {
        return null;
    }

    @Override
    public List<String> extractSummary(String content) {
        return extractSummary(content, 6);
    }

    public List<String> extractSummary(String content, int num) {
        List<String> candidateSentences = generateCandidateSentences(content);
        List<Sentence> summarySentences = generateSummarySentences(candidateSentences);
        return summarySentences.stream().limit(num)
            .sorted(Comparator.comparingInt(Sentence::getIdx))
            .map(Sentence::getSentence)
            .map(NlpUtil::removeNumPrefix)
            .collect(Collectors.toList());
    }

    @Override
    public List<String> extractKeywords(String content) {
        return extractKeywords("", content);
    }

    @Override
    public List<String> extractKeywords(String title, String content) {
        List<KeywordResult> keywords = extractKeywords(title, content, 8, 0.2);
        return keywords.stream().map(KeywordResult::getWord).collect(Collectors.toList());
    }

    public List<String> extractKeywordsCustom(String title, String content, int topK, double fraction) {
        List<KeywordResult> keywords = extractKeywords(title, content, topK, fraction);
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
                .filter(keyword -> keyword.isInTitle() || keyword.getFrequency() >= 2 || keyword.getWeight() > 0.4)
                .map(keyword -> {
                    boolean similarityFilter = false;
                    if (wordVecs.containsKey(keyword.getWord())) {
                        float titleSimilarity = Distance.cosine(titleEmbedding, wordVecs.get(keyword.getWord()));
                        float contentSimilarity = Distance.cosine(docEmbedding, wordVecs.get(keyword.getWord()));

                        similarityFilter = similarFilterList.stream()
                            .map(pair -> titleSimilarity >= pair.getValue0() && contentSimilarity >= pair.getValue1())
                            .reduce((a, b) -> a || b).orElse(false);
                        keyword.setSimilarity((titleSimilarity + contentSimilarity) / 2.0F);
                    }
                    if (keyword.getWeight() > 0.4 || (keyword.isInTitle() && keyword.getWeight() > 0.11) || similarityFilter) {
                        return keyword;
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(k -> -k.getWeight() * 3 - k.getSimilarity()))
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
            .collect(Collectors.toMap(Pair::getValue0, Pair::getValue1));

        int tfSum = allWords.size();

        List<KeywordResult> sortedKeywordsList = allWords.stream()
            .filter(wordObj -> phrases.contains(wordObj.getWord()) && !filterByDict(wordObj.getWord()))
            .map(wordObj -> {
                double tf = wordFreq.getOrDefault(wordObj.getWord(), 0) * 1.0 / (tfSum + 1.0);
                tf = Math.min(tf, 0.06);
                double idf = mixIdf.getOrDefault(wordObj.getWord(), DEFAULT_IDF);
                double tfidf = tf * Math.pow(idf, 3.0) / 30;

                double titleScore = 0;
                if (titleWordSet.contains(wordObj.getWord()) && wordObj.getLen() > 0) {
                    if (IMPORTANT_NOUN.contains(wordObj.getPosTag())) {
                        titleScore = 0.8;
                    } else if (NOUN.contains(wordObj.getWord()) && wordObj.getLen() > 0) {
                        titleScore = Math.min(2, wordObj.getLen() - 1) * 0.2;
                    } else {
                        titleScore = Math.min(3.5, wordObj.getLen() - 1) * 0.1;
                    }
                }

                double lenScore = wordObj.getLen() * 1.0 / (wordObj.getLen() + 4);

                double speechScore = 0;
                if (IMPORTANT_NOUN.contains(wordObj.getPosTag())) {
                    speechScore = 0.6;
                } else if (NOUN.contains(wordObj.getPosTag())) {
                    speechScore = 0.15;
                } else if (VERB.contains(wordObj.getPosTag())) {
                    speechScore = 0.1;
                }

                double gridKeywordsScore = 0;
                if (gridKeywords.contains(wordObj.getWord())) {
                    gridKeywordsScore = 1;
                }

                double weight = tfidf * 0.7 + titleScore * 0.15 + lenScore * 0.06 + speechScore * 0.06 + gridKeywordsScore * 0.4;

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
        return allStopwords.contains(word) || filterWords.contains(word) || StringUtils.isNumeric(word) || StringUtils.containsAny(word, "0123456789") || word.length() <= 1;
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

    private List<String> generateCandidateSentences(String content) {
        List<String> sentences = NlpUtil.toSentence(content);
        List<String> candidateSentences = sentences.stream()
            .filter(x -> {
                boolean matchSummaryWords = !summaryMatcher.matching(x, false).isEmpty();
                boolean noTime = !x.contains("发布时间");
                boolean noFujian = !x.contains("附件");
                boolean noNeibu = !x.contains("内部事项");
                return matchSummaryWords && noTime && noFujian && noNeibu;
            })
            .collect(Collectors.toList());
        return candidateSentences;
    }

    public List<Sentence> generateSummarySentences(List<String> sentences) {
        float[][] textGraph = generateTextGraph(sentences);
        float[] sentenceScores = calculateScoreByTextRank(textGraph);
        int idx = 0;
        List<Sentence> sentenceList = new LinkedList<>();
        for (float score : sentenceScores) {
            String sentence = sentences.get(idx);
            Sentence s = new Sentence(sentence, idx, score);
            sentenceList.add(s);
            idx++;
        }
        return sentenceList.stream().sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
            .collect(Collectors.toList());
    }

    private float[] calculateScoreByTextRank(float[][] textGraph) {
        return calculateScoreByTextRank(textGraph, 20, 0.6f, 0.002f);
    }

    private float[] calculateScoreByTextRank(float[][] textGraph, int iterNums, float dampingFactor, float minDiff) {
        int sentenceNums = textGraph.length;
        float[] weightSum = new float[sentenceNums];
        float initialSentenceScore = sentenceNums != 0 ? 1f / sentenceNums : 1f;
        float[] sentenceScores = new float[sentenceNums];
        for (int i = 0; i < sentenceNums; i++) {
            sentenceScores[i] = initialSentenceScore;
        }
        float scoreDiff = Float.MAX_VALUE;
        int epoch = 0;

        for (int i = 0; i < sentenceNums; i++) {
            float sum = 0;
            for (int j = 0; j < textGraph[i].length; j++) {
                sum += textGraph[i][j];
            }
            weightSum[i] = sum;
        }

        while (scoreDiff >= minDiff && epoch <= iterNums) {
            float[] sentenceScoreTemp = new float[sentenceNums];
            for (int i = 0; i < sentenceNums; i++) {
                sentenceScoreTemp[i] = 0;
            }
            scoreDiff = 0;
            for (int i = 0; i < sentenceNums; i++) {
                for (int j = 0; j < sentenceNums; j++) {
                    if (weightSum[j] != 0) {
                        sentenceScoreTemp[i] += textGraph[i][j] / weightSum[j] * sentenceScores[j];
                    }
                }
                sentenceScoreTemp[i] = sentenceScoreTemp[i] * dampingFactor + sentenceScores[i] * (1 - dampingFactor);
                float diff = Math.abs(sentenceScoreTemp[i] - sentenceScores[i]);
                scoreDiff = Math.max(diff, scoreDiff);
            }

            sentenceScores = sentenceScoreTemp;
            epoch += 1;
        }
        return sentenceScores;
    }

    private float[][] generateTextGraph(List<String> sentences) {
        int length = sentences.size();
        float[][] textGraph = new float[length][length];
        List<Map<String, Integer>> tfScores = sentences.stream().map(this::countsTf).collect(Collectors.toList());

        for (int i = 0; i < length; i++) {
            for (int j = i; j < length; j++) {
                Map<String, Integer> tfMap1 = tfScores.get(i);
                Map<String, Integer> tfMap2 = tfScores.get(j);
                float similarity = 0;
                if (i != j) {
                    similarity = (float) calculateModifiedTfIdfScore(tfMap1, tfMap2);
                }
                textGraph[i][j] = similarity;
                textGraph[j][i] = similarity;
            }
        }
        return textGraph;
    }

    private Map<String, Integer> countsTf(String sentence) {
        List<Term> words = hanlpSegment.seg(sentence);
        return words.stream().map(term -> term.word)
            .collect(Collectors.groupingBy(x -> x))
            .entrySet()
            .stream().map(x -> {
                String word = x.getKey();
                int cnt = x.getValue().size();
                return new Pair<>(word, cnt);
            })
            .collect(Collectors.toMap(Pair::getValue0, Pair::getValue1));
    }

    private double calculateModifiedTfIdfScore(Map<String, Integer> tfMap1, Map<String, Integer> tfMap2) {
        Set<String> wordSet = new HashSet<>(tfMap1.keySet());
        wordSet.addAll(tfMap2.keySet());
        return wordSet.stream().map(word -> {
            double score = 0;
            if (mixIdf.containsKey(word)) {
                int tf1 = tfMap1.getOrDefault(word, 0);
                int tf2 = tfMap2.getOrDefault(word, 0);
                float idf = mixIdf.getOrDefault(word, DEFAULT_IDF);
                score = tf1 * tf2 * Math.pow(idf, 2);
            }
            return score;
        }).mapToDouble(x -> x).average().orElse(0);
    }

}
