package com.exam.practice.service;

import com.exam.common.BizException;
import com.exam.common.ScoreRules;
import com.exam.question.entity.Question;
import com.exam.question.mapper.QuestionMapper;
import com.exam.practice.entity.PracticeRecord;
import com.exam.practice.mapper.PracticeMapper;
import com.exam.security.AuthUser;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PracticeService {

    private final QuestionMapper questionMapper;
    private final PracticeMapper practiceMapper;
    private final StringRedisTemplate redis;

    public Map<String, Object> next(Long categoryId, String type) {
        Question question = questionMapper.randomOne(categoryId, type);
        if (question == null) {
            throw new BizException("当前条件下没有可练习的题目");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", question.getId());
        body.put("type", question.getType());
        body.put("difficulty", question.getDifficulty());
        body.put("stem", question.getStem());
        body.put("optionsJson", question.getOptionsJson());
        body.put("score", question.getScore());
        return body;
    }

    @Transactional
    public JudgeResult submit(Long questionId, String userAnswer) {
        Question question = questionMapper.selectById(questionId);
        if (question == null || (question.getDeleted() != null && question.getDeleted() == 1)) {
            throw new BizException(404, "题目不存在");
        }
        int got = "SHORT".equals(question.getType())
                ? ScoreRules.keyword(question.getKeywords(), userAnswer, question.getScore())
                : ScoreRules.objective(question.getType(), question.getAnswer(), userAnswer, question.getScore());
        PracticeRecord record = new PracticeRecord();
        record.setUserId(AuthUser.require().id());
        record.setQuestionId(questionId);
        record.setUserAnswer(userAnswer == null ? "" : userAnswer.trim());
        record.setCorrect(got == question.getScore() ? 1 : 0);
        practiceMapper.insert(record);
        redis.delete("question:hot");
        JudgeResult result = new JudgeResult();
        result.setCorrect(record.getCorrect() == 1);
        result.setScore(got);
        result.setFullScore(question.getScore());
        result.setAnswer(question.getAnswer());
        result.setAnalysis(question.getAnalysis());
        return result;
    }

    public Map<String, Object> summary() {
        Map<String, Object> raw = practiceMapper.summary(AuthUser.require().id());
        long total = number(raw == null ? null : raw.get("total"));
        long hit = number(raw == null ? null : raw.get("hit"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("total", total);
        body.put("hit", hit);
        body.put("accuracy", total == 0 ? 0 : (int) (hit * 100 / total));
        return body;
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    @Data
    public static class JudgeResult {
        private boolean correct;
        private int score;
        private int fullScore;
        private String answer;
        private String analysis;
    }
}
