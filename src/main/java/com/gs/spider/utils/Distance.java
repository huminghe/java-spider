package com.gs.spider.utils;

import com.github.fommil.netlib.BLAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author huminghe
 * @date 2021/7/20
 */
public class Distance {

    private static final Logger LOG = LoggerFactory.getLogger(Distance.class);

    private static final BLAS blas = BLAS.getInstance();

    public static float cosine(float[] v1, float[] v2) {
        if (v1.length == 0 || v2.length == 0 || v1.length != v2.length) {
            return 0.0f;
        } else {
            int len = v1.length;
            return blas.snrm2(len, v1, 1) * blas.snrm2(len, v2, 1);
        }
    }

    public static float cosine(List<Float> v1, List<Float> v2) {
        if (v1.isEmpty() || v2.isEmpty() || v1.size() != v2.size()) {
            return 0.0f;
        }
        int len = v1.size();
        float[] v1Array = new float[len];
        int i = 0;
        for (Float f : v1) {
            v1Array[i] = (f != null ? f : Float.NaN);
            i++;
        }
        len = v2.size();
        float[] v2Array = new float[len];
        i = 0;
        for (Float f : v2) {
            v2Array[i] = (f != null ? f : Float.NaN);
            i++;
        }
        return cosine(v1Array, v2Array);
    }

    public static float[] wordsToVector(String[] words, Map<String, float[]> wordVecMap) {
        int size = wordVecMap.values().stream().findFirst().orElse(new float[0]).length;
        float[] result = new float[size];
        int count = words.length;
        Arrays.stream(words).forEach(w -> {
            float[] vec = wordVecMap.get(w);
            if (vec != null) {
                blas.saxpy(size, 1F / count, vec, 1, result, 1);
            }
        });
        return result;
    }

    public static float[] wordsToVector(List<String> words, Map<String, float[]> wordVecMap) {
        int size = words.size();
        String[] wordsArray = new String[size];
        String[] wordsArr = words.toArray(wordsArray);
        return wordsToVector(wordsArr, wordVecMap);
    }

}
