package com.exam.paper;

import com.exam.common.BizException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 按规则抽题。同一题不重复，抽出题目的分值之和必须等于设定总分。
 */
public final class PaperPicker {

    private PaperPicker() {
    }

    public record Rule(long categoryId, String type, int count, int scoreEach) {
    }

    public record Picked(long questionId, int score) {
    }

    public static List<Picked> pick(List<Rule> rules, List<List<Long>> pools, int expectedTotal, Random random) {
        if (rules == null || rules.isEmpty()) {
            throw new BizException("组卷规则不能为空");
        }
        if (pools == null || pools.size() != rules.size()) {
            throw new BizException("题库与规则数量不一致");
        }
        int planned = 0;
        for (Rule rule : rules) {
            if (rule.count() <= 0 || rule.scoreEach() <= 0) {
                throw new BizException("抽题数量和每题分值必须为正整数");
            }
            planned += rule.count() * rule.scoreEach();
        }
        if (planned != expectedTotal) {
            throw new BizException("总分与设定偏差不为 0");
        }
        Set<Long> used = new HashSet<>();
        List<Picked> picked = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            Rule rule = rules.get(i);
            List<Long> pool = new ArrayList<>(pools.get(i));
            pool.removeIf(used::contains);
            if (pool.size() < rule.count()) {
                throw new BizException("分类 " + rule.categoryId() + " 的 " + rule.type() + " 题目数量不足");
            }
            Collections.shuffle(pool, random);
            for (int n = 0; n < rule.count(); n++) {
                long id = pool.get(n);
                used.add(id);
                picked.add(new Picked(id, rule.scoreEach()));
            }
        }
        int sum = picked.stream().mapToInt(Picked::score).sum();
        if (sum != expectedTotal || used.size() != picked.size()) {
            throw new BizException("组卷结果未通过总分或去重校验");
        }
        return picked;
    }

    public static void checkManual(int totalScore, List<Picked> items) {
        if (items == null || items.isEmpty()) {
            throw new BizException("试卷至少包含一道题");
        }
        Set<Long> ids = new HashSet<>();
        int sum = 0;
        for (Picked item : items) {
            if (item.score() <= 0) {
                throw new BizException("题目分值必须为正整数");
            }
            if (!ids.add(item.questionId())) {
                throw new BizException("试卷中存在重复题目");
            }
            sum += item.score();
        }
        if (sum != totalScore) {
            throw new BizException("总分与设定偏差不为 0，当前合计 " + sum);
        }
    }
}
