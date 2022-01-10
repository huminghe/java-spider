package com.gs.spider.model.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author huminghe
 * @date 2022/1/4
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NerResult {
    private String word;
    private int category;
}
