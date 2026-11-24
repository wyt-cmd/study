package com.exam.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreRulesTest {

    @Test
    void objectiveBoundaries() {
        assertEquals(5, ScoreRules.objective("SINGLE", "a", " A ", 5));
        assertEquals(0, ScoreRules.objective("SINGLE", "A", "B", 5));
        assertEquals(5, ScoreRules.objective("MULTI", "B,A", "a|b", 5));
        assertEquals(0, ScoreRules.objective("MULTI", "A,B", "A", 5));
        assertEquals(2, ScoreRules.objective("JUDGE", "正确", "true", 2));
        assertEquals(0, ScoreRules.objective("JUDGE", "对", "错", 2));
        assertEquals(0, ScoreRules.objective("SINGLE", "A", "", 3));
        assertEquals(1, ScoreRules.objective("SINGLE", "A", "A", 1));
    }

    @Test
    void keywordUsesIntegerRounding() {
        assertEquals(0, ScoreRules.keyword("事务,隔离,锁", "", 10));
        assertEquals(3, ScoreRules.keyword("事务,隔离,锁", "事务", 10));
        assertEquals(7, ScoreRules.keyword("事务,隔离,锁", "事务和隔离", 10));
        assertEquals(10, ScoreRules.keyword("事务,隔离,锁", "事务隔离锁", 10));
        assertEquals(0, ScoreRules.keyword("", "任意", 10));
        assertEquals(0, ScoreRules.clamp(-1, 10));
        assertEquals(4, ScoreRules.clamp(9, 4));
    }

    @Test
    void negativeScoreRejected() {
        assertThrows(BizException.class, () -> ScoreRules.objective("SINGLE", "A", "A", -1));
    }
}
