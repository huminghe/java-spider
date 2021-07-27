package com.gs.spider.model.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author huminghe
 * @date 2021/7/20
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeywordResult {
    private String word;
    private double weight;
    private int frequency;
    private boolean inTitle;
    private float similarity;
}