package com.gs.spider.utils;

import com.gs.spider.model.utils.MatchHit;
import com.hankcs.hanlp.collection.AhoCorasick.AhoCorasickDoubleArrayTrie;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author huminghe
 * @date 2021/7/19
 */
@Data
@AllArgsConstructor
public class AhoCorasickMatcher<V> {

    private Logger LOG = LoggerFactory.getLogger(getClass());

    private Map<String, V> dict;
    private AhoCorasickDoubleArrayTrie<V> acdat = new AhoCorasickDoubleArrayTrie<>();

    public AhoCorasickMatcher(Map<String, V> dict) {
        this.dict = dict;
        acdat.build(new TreeMap<>(dict));
    }

    public List<MatchHit<V>> matching(String input, Boolean ignoreOverlap) {
        try {
            List<AhoCorasickDoubleArrayTrie<V>.Hit<V>> matched = acdat.parseText(input);
            if (ignoreOverlap) {
                int preEnd = -1;
                int preStart = -1;
                List<MatchHit<V>> res = new LinkedList<>();
                List<AhoCorasickDoubleArrayTrie<V>.Hit<V>> sortedList = matched
                    .stream().sorted((o1, o2) -> {
                        int s1 = Integer.compare(o1.begin, o2.begin);
                        if (s1 != 0) {
                            return s1;
                        } else {
                            return Integer.compare(o2.end, o1.end);
                        }
                    })
                    .collect(Collectors.toList());

                for (AhoCorasickDoubleArrayTrie<V>.Hit<V> entity : sortedList) {
                    if (entity.begin != preEnd && entity.begin >= preStart) {
                        preEnd = entity.begin;
                        preStart = entity.end;
                        res.add(new MatchHit<>(entity.begin, entity.end, entity.value));
                    }
                }
                return res;
            } else {
                return matched.stream()
                    .map(entity -> new MatchHit<>(entity.begin, entity.end, entity.value))
                    .collect(Collectors.toList());
            }
        } catch (Exception ex) {
            LOG.error("matching error ", ex);
            return new LinkedList<>();
        }
    }

}
