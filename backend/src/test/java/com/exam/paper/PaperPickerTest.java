package com.exam.paper;

import com.exam.common.BizException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperPickerTest {

    @Test
    void pickHasNoDuplicateAndExactTotal() {
        List<PaperPicker.Rule> rules = List.of(
                new PaperPicker.Rule(1, "SINGLE", 2, 5),
                new PaperPicker.Rule(1, "MULTI", 1, 10)
        );
        List<List<Long>> pools = List.of(List.of(1L, 2L, 3L), List.of(2L, 4L));
        List<PaperPicker.Picked> picked = PaperPicker.pick(rules, pools, 20, new Random(1));
        assertEquals(3, picked.size());
        assertEquals(20, picked.stream().mapToInt(PaperPicker.Picked::score).sum());
        assertEquals(3, picked.stream().map(PaperPicker.Picked::questionId).distinct().count());
        assertTrue(picked.stream().noneMatch(item -> item.questionId() == 2 && item.score() == 10
                && picked.stream().anyMatch(other -> other.questionId() == 2 && other.score() == 5)));
    }

    @Test
    void rejectsShortPoolAndScoreDrift() {
        List<PaperPicker.Rule> rules = List.of(new PaperPicker.Rule(1, "SINGLE", 3, 5));
        assertThrows(BizException.class, () -> PaperPicker.pick(rules, List.of(List.of(1L, 2L)), 15, new Random(1)));
        assertThrows(BizException.class, () -> PaperPicker.pick(rules, List.of(List.of(1L, 2L, 3L)), 14, new Random(1)));
    }

    @Test
    void manualRejectsDuplicateAndMismatch() {
        assertThrows(BizException.class, () -> PaperPicker.checkManual(10, List.of(
                new PaperPicker.Picked(1, 5),
                new PaperPicker.Picked(1, 5)
        )));
        assertThrows(BizException.class, () -> PaperPicker.checkManual(10, List.of(
                new PaperPicker.Picked(1, 4),
                new PaperPicker.Picked(2, 5)
        )));
    }
}
