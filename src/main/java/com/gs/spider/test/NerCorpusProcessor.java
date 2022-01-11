package com.gs.spider.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author huminghe
 * @date 2022/1/11
 */
public class NerCorpusProcessor {
    public static void main(String[] args) {
        List<String> contentList = new LinkedList<>();

        String path = "/Users/huminghe/Documents/tmp/1115/ner_corpus_new";
        String outputPath = "/Users/huminghe/Documents/tmp/1115/ner_data/ner";

        File file = new File(path);
        File[] fs = file.listFiles();

        try {
            for (File f : fs) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    contentList.add(line.trim());
                }
                reader.close();
            }

            Collections.shuffle(contentList);

            int idx = 1;
            int stat = 0;

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath + idx + ".txt"), StandardCharsets.UTF_8));

            for (String content : contentList) {

                if (stat >= 10000) {
                    writer.close();
                    idx++;
                    stat = 0;
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath + idx + ".txt"), StandardCharsets.UTF_8));
                }
                stat++;
                writer.write(content);
                writer.newLine();
            }
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
