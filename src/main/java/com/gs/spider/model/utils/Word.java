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
public class Word {
    private String word;
    private int posBegin;
    private int len;
    private String posTag;
}
