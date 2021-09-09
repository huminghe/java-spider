package com.gs.spider.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author huminghe
 * @date 2021/7/21
 */
public class Loader {

    private static final Logger LOG = LoggerFactory.getLogger(Loader.class);

    public static Set<String> load(String fileName) {
        return load(fileName, 0);
    }

    public static Set<String> load(String fileName, int type) {
        InputStream inStream = Loader.class.getResourceAsStream(fileName);

        Set<String> resultSet = new HashSet<>();

        LOG.info("loading resources from : " + fileName);
        try {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
            while ((line = reader.readLine()) != null) {
                String content = "";
                if (type == 0) {
                    content = line.trim();
                } else if (type == 1) {
                    content = StringUtils.join(Arrays.stream(line.trim().split("\\s+")).skip(1), " ").toLowerCase();
                }
                if (StringUtils.isNotEmpty(content)) {
                    resultSet.add(content);
                }
            }
        } catch (Exception ex) {
            LOG.error("load resources from " + fileName, ex);
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch (Exception e) {
                LOG.error("failed to close file: " + fileName, e);
            }
        }
        return resultSet;
    }

    public static Map<String, Float> loadIdf(String fileName) {
        InputStream inStream = Loader.class.getResourceAsStream(fileName);

        Map<String, Float> result = new HashMap<>();

        LOG.info("loading idf from : " + fileName);
        try {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
            while ((line = reader.readLine()) != null) {
                try {
                    String[] pieces = line.split("\t");
                    String word = pieces[0];
                    float idf = Float.parseFloat(pieces[2]);
                    result.put(word, idf);
                } catch (Exception ex) {
                    LOG.error("parse line error: " + line, ex);
                }
            }
        } catch (Exception ex) {
            LOG.error("load idf from " + fileName, ex);
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch (Exception e) {
                LOG.error("failed to close file: " + fileName, e);
            }
        }
        return result;
    }

    public static Map<String, String> loadMapping(String fileName) {
        InputStream inStream = Loader.class.getResourceAsStream(fileName);
        Map<String, String> result = new HashMap<>();
        LOG.info("loading mapping from: " + fileName);
        try {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
            while ((line = reader.readLine()) != null) {
                try {
                    String[] pieces = line.split("\t");
                    String domain = pieces[0];
                    String name = pieces[1];
                    result.put(domain, name);
                } catch (Exception ex) {
                    LOG.error("parse line error: " + line, ex);
                }
            }
        } catch (Exception ex) {
            LOG.error("load mapping from " + fileName, ex);
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch (Exception e) {
                LOG.error("failed to close file: " + fileName, e);
            }
        }
        return result;
    }

    public static Map<String, float[]> loadWordVecs(String filePath, int dim) {
        InputStream inStream = null;
        Map<String, float[]> result = new HashMap<>();
        LOG.info("loading word vecs from: " + filePath);
        try {
            inStream = new FileInputStream(filePath);
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
            while ((line = reader.readLine()) != null) {
                try {
                    String[] pieces = line.split("\\s+");
                    String word = pieces[0];
                    float[] vec = new float[dim];
                    List<String> vecs = Arrays.stream(pieces).skip(1).collect(Collectors.toList());
                    int idx = 0;
                    for (String value : vecs) {
                        vec[idx] = Float.parseFloat(value);
                        idx++;
                    }
                    result.put(word, vec);
                } catch (Exception ex) {
                    LOG.error("parse line error: " + line, ex);
                }
            }
        } catch (Exception ex) {
            LOG.error("load word vecs from " + filePath, ex);
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch (Exception e) {
                LOG.error("failed to close file: " + filePath, e);
            }
        }
        return result;
    }

}
