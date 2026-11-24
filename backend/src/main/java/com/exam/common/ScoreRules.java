package com.exam.common;

import java.util.Arrays;
import java.util.Locale;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 分数全部用整数运算。主观题部分得分采用四舍五入，不用 double。
 */
public final class ScoreRules {

    private ScoreRules() {
    }

    public static int objective(String type, String standard, String userAnswer, int fullScore) {
        if (fullScore < 0) {
            throw new BizException("分值不能为负");
        }
        String expected = normalizeObjective(type, standard);
        String actual = normalizeObjective(type, userAnswer);
        return !expected.isEmpty() && expected.equals(actual) ? fullScore : 0;
    }

    public static int keyword(String keywords, String userAnswer, int fullScore) {
        if (fullScore < 0) {
            throw new BizException("分值不能为负");
        }
        String[] parts = keywords == null ? new String[0] : keywords.split("[,，]");
        String[] keys = Arrays.stream(parts).map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        if (keys.length == 0) {
            return 0;
        }
        String text = userAnswer == null ? "" : userAnswer;
        int hit = 0;
        for (String key : keys) {
            if (text.contains(key)) {
                hit++;
            }
        }
        if (hit == 0) {
            return 0;
        }
        if (hit == keys.length) {
            return fullScore;
        }
        return (int) ((fullScore * (long) hit + keys.length / 2) / keys.length);
    }

    public static int clamp(int score, int fullScore) {
        if (score < 0) {
            return 0;
        }
        return Math.min(score, fullScore);
    }

    public static String normalizeObjective(String type, String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return "";
        }
        if ("JUDGE".equals(type)) {
            String lower = value.toLowerCase(Locale.ROOT);
            if (lower.equals("t") || lower.equals("true") || value.equals("对") || value.equals("正确") || value.equals("是")) {
                return "T";
            }
            if (lower.equals("f") || lower.equals("false") || value.equals("错") || value.equals("错误") || value.equals("否")) {
                return "F";
            }
            return value;
        }
        if ("MULTI".equals(type)) {
            return Arrays.stream(value.split("[,，|、]"))
                    .map(s -> s.trim().toUpperCase(Locale.ROOT))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toCollection(TreeSet::new))
                    .stream()
                    .collect(Collectors.joining(","));
        }
        return value.toUpperCase(Locale.ROOT);
    }
}
