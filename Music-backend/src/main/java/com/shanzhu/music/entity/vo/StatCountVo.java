package com.shanzhu.music.entity.vo;

import lombok.Data;

/**
 * 一组"取值 + 行数"的聚合结果，只用于统计出参。
 *
 * label 是被 GROUP BY 那一列的原文（性别码、地区、风格），total 是该组的行数。
 * 之所以不直接返回 Map，是为了让出参保持数组形态：分组列的取值来自库里实际存在的
 * 文本，前端拿到的是"有哪些值、各多少"，不必去猜一个动态键集合。
 */
@Data
public class StatCountVo {

    /**
     * 分组列的取值
     */
    private String label;

    /**
     * 该取值对应的行数
     */
    private Long total;
}
