package com.gs.spider.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author huminghe
 * @date 2022/2/11
 */
public class DeduplicateData {

    public static void main(String[] args) {
        Set<String> contentSet = new HashSet<>();

        String path = "/Users/huminghe/Documents/22tmp/0211/22.txt";
        String outputPath = "/Users/huminghe/Documents/22tmp/0211/relation_extraction_data.txt";

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            String line = null;
            while ((line = reader.readLine()) != null) {
                String lineModified = line.trim().replaceAll("\u3000", "");
                if (!lineModified.contains("\"") && !lineModified.contains("'")) {
                    contentSet.add(lineModified);
                }
            }
            reader.close();
            List<String> contentList = new LinkedList<>(contentSet);
            Collections.shuffle(contentList);
            contentList = contentList.subList(0, 10000);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath), StandardCharsets.UTF_8));
            for (String content : contentList) {
                writer.write(content);
                writer.newLine();
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
