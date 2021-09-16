package com.gs.spider.model.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author huminghe
 * @date 2021/9/16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Sentence {
    private String sentence;
    private int idx;
    private float score;
}
