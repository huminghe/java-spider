package com.gs.spider.model.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author huminghe
 * @date 2022/2/8
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SentencesInfo {
    private List<String> sentences;
    private int num;
}
