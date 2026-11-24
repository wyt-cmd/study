package com.exam.record.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.ai.AiClient;
import com.exam.common.BizException;
import com.exam.common.ScoreRules;
import com.exam.paper.entity.Paper;
import com.exam.paper.entity.PaperQuestion;
import com.exam.paper.service.PaperService;
import com.exam.question.entity.Question;
import com.exam.question.mapper.QuestionMapper;
import com.exam.record.dto.ExamDtos;
import com.exam.record.entity.ExamAnswer;
import com.exam.record.entity.ExamRecord;
import com.exam.record.mapper.ExamAnswerMapper;
import com.exam.record.mapper.ExamRecordMapper;
import com.exam.security.AuthUser;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRecordMapper recordMapper;
    private final ExamAnswerMapper answerMapper;
    private final PaperService paperService;
    private final QuestionMapper questionMapper;
    private final AiClient aiClient;
    private final PlatformTransactionManager transactionManager;

    @Transactional
    public ExamDtos.SessionView start(Long paperId) {
        Paper paper = paperService.require(paperId);
        if (!"PUBLISHED".equals(paper.getStatus())) {
            throw new BizException("试卷未发布，不能开考");
        }
        AuthUser user = AuthUser.require();
        ExamRecord existing = recordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, user.id())
                .eq(ExamRecord::getPaperId, paperId));
        if (existing != null) {
            if ("ONGOING".equals(existing.getStatus()) && existing.getDeadline().isAfter(LocalDateTime.now())) {
                return view(existing, false);
            }
            if ("ONGOING".equals(existing.getStatus())) {
                gradeLocked(lock(existing.getId()), true);
            }
            throw new BizException("同一考生不可重复开考");
        }
        ExamRecord record = new ExamRecord();
        record.setPaperId(paperId);
        record.setUserId(user.id());
        record.setStartTime(LocalDateTime.now());
        record.setDeadline(record.getStartTime().plusMinutes(paper.getDurationMin()));
        record.setStatus("ONGOING");
        try {
            recordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            throw new BizException("同一考生不可重复开考");
        }
        return view(record, false);
    }

    public ExamDtos.SessionView detail(Long recordId) {
        ExamRecord record = own(recordId);
        if ("ONGOING".equals(record.getStatus()) && !record.getDeadline().isAfter(LocalDateTime.now())) {
            runInTx(() -> closeIfExpired(recordId));
            record = recordMapper.selectById(recordId);
        }
        boolean finished = !"ONGOING".equals(record.getStatus());
        return view(record, finished);
    }

    @Transactional
    public void saveAnswers(Long recordId, List<ExamDtos.AnswerItem> answers) {
        ExamRecord record = lock(recordId);
        assertOngoing(record);
        if (answers == null || answers.isEmpty()) {
            return;
        }
        Map<Long, ExamAnswer> saved = answersOf(recordId);
        for (ExamDtos.AnswerItem item : answers) {
            ExamAnswer answer = saved.get(item.getQuestionId());
            if (answer == null) {
                answer = new ExamAnswer();
                answer.setRecordId(recordId);
                answer.setQuestionId(item.getQuestionId());
                answer.setUserAnswer(trim(item.getUserAnswer()));
                answer.setScore(0);
                answer.setCorrect(0);
                answerMapper.insert(answer);
            } else {
                answer.setUserAnswer(trim(item.getUserAnswer()));
                answerMapper.updateById(answer);
            }
        }
    }

    @Transactional
    public ExamDtos.SessionView submit(Long recordId) {
        ExamRecord record = lock(recordId);
        if (!record.getUserId().equals(AuthUser.require().id()) && !staff()) {
            throw new BizException(403, "越权访问");
        }
        if (!"ONGOING".equals(record.getStatus())) {
            throw new BizException("试卷已提交");
        }
        boolean timeout = !record.getDeadline().isAfter(LocalDateTime.now());
        gradeLocked(record, timeout);
        return view(recordMapper.selectById(recordId), true);
    }

    public void sweepTimeouts() {
        List<ExamRecord> expired = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getStatus, "ONGOING")
                .lt(ExamRecord::getDeadline, LocalDateTime.now())
                .last("LIMIT 200"));
        for (ExamRecord item : expired) {
            try {
                runInTx(() -> closeIfExpired(item.getId()));
            } catch (RuntimeException ex) {
                log.warn("超时收卷失败 record={}: {}", item.getId(), ex.getMessage());
            }
        }
    }

    private void closeIfExpired(Long recordId) {
        ExamRecord record = recordMapper.lockById(recordId);
        if (record != null && "ONGOING".equals(record.getStatus()) && !record.getDeadline().isAfter(LocalDateTime.now())) {
            gradeLocked(record, true);
        }
    }

    private void runInTx(Runnable action) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
    }

    private void gradeLocked(ExamRecord record, boolean timeout) {
        List<PaperQuestion> links = paperService.links(record.getPaperId());
        Map<Long, ExamAnswer> saved = answersOf(record.getId());
        List<Long> questionIds = links.stream().map(PaperQuestion::getQuestionId).toList();
        Map<Long, Question> questions = questionIds.isEmpty() ? Map.of() : questionMapper.selectBatchIds(questionIds)
                .stream().collect(Collectors.toMap(Question::getId, Function.identity()));
        int objective = 0;
        int subjective = 0;
        List<String> comments = new ArrayList<>();
        for (PaperQuestion link : links) {
            Question question = questions.get(link.getQuestionId());
            if (question == null) {
                continue;
            }
            ExamAnswer answer = saved.get(question.getId());
            String userAnswer = answer == null ? "" : answer.getUserAnswer();
            int got;
            String comment;
            if ("SHORT".equals(question.getType())) {
                int ruleScore = ScoreRules.keyword(question.getKeywords(), userAnswer, link.getScore());
                Optional<JsonNode> ai = gradeByAi(question, userAnswer, link.getScore());
                if (ai.isPresent()) {
                    got = ScoreRules.clamp(ai.get().path("score").asInt(ruleScore), link.getScore());
                    comment = ai.get().path("comment").asText("AI 评分");
                } else {
                    got = ruleScore;
                    comment = "按关键词命中评分";
                }
                subjective += got;
                comments.add("第" + link.getSortNo() + "题：" + comment);
            } else {
                got = ScoreRules.objective(question.getType(), question.getAnswer(), userAnswer, link.getScore());
                comment = got == link.getScore() ? "正确" : "错误";
                objective += got;
            }
            if (answer == null) {
                answer = new ExamAnswer();
                answer.setRecordId(record.getId());
                answer.setQuestionId(question.getId());
                answer.setUserAnswer(userAnswer);
                answer.setScore(got);
                answer.setCorrect(got == link.getScore() ? 1 : 0);
                answer.setComment(comment);
                answerMapper.insert(answer);
            } else {
                answer.setScore(got);
                answer.setCorrect(got == link.getScore() ? 1 : 0);
                answer.setComment(comment);
                answerMapper.updateById(answer);
            }
        }
        record.setObjectiveScore(objective);
        record.setSubjectiveScore(subjective);
        record.setScore(objective + subjective);
        record.setStatus(timeout ? "TIMEOUT" : "SUBMITTED");
        record.setSubmitTime(timeout ? record.getDeadline() : LocalDateTime.now());
        record.setAiComment(comments.isEmpty() ? null : String.join("；", comments));
        recordMapper.updateById(record);
    }

    private Optional<JsonNode> gradeByAi(Question question, String userAnswer, int fullScore) {
        String prompt = """
                你是阅卷老师。只返回 JSON：{"score":整数,"comment":"一句话"}。
                满分是 %d，score 必须是 0 到满分之间的整数。
                题干：%s
                参考答案：%s
                关键词：%s
                考生答案：%s
                """.formatted(fullScore, question.getStem(), question.getAnswer(),
                question.getKeywords() == null ? "" : question.getKeywords(),
                userAnswer == null ? "" : userAnswer);
        return aiClient.tryChat(prompt);
    }

    private ExamDtos.SessionView view(ExamRecord record, boolean showAnswer) {
        Paper paper = paperService.require(record.getPaperId());
        List<PaperQuestion> links = paperService.links(record.getPaperId());
        Map<Long, Question> questions = links.isEmpty() ? Map.of() : questionMapper.selectBatchIds(
                links.stream().map(PaperQuestion::getQuestionId).toList()
        ).stream().collect(Collectors.toMap(Question::getId, Function.identity()));
        Map<Long, ExamAnswer> saved = answersOf(record.getId());
        ExamDtos.SessionView view = new ExamDtos.SessionView();
        view.setRecordId(record.getId());
        view.setPaperId(paper.getId());
        view.setPaperTitle(paper.getTitle());
        view.setDurationMin(paper.getDurationMin());
        view.setTotalScore(paper.getTotalScore());
        view.setPassScore(paper.getPassScore());
        view.setStatus(record.getStatus());
        view.setStartTime(record.getStartTime());
        view.setDeadline(record.getDeadline());
        view.setSubmitTime(record.getSubmitTime());
        view.setScore(record.getScore());
        view.setObjectiveScore(record.getObjectiveScore());
        view.setSubjectiveScore(record.getSubjectiveScore());
        view.setAiComment(showAnswer ? record.getAiComment() : null);
        view.setShowAnswer(showAnswer);
        List<ExamDtos.QuestionView> items = new ArrayList<>();
        for (PaperQuestion link : links) {
            Question question = questions.get(link.getQuestionId());
            if (question == null) {
                continue;
            }
            ExamAnswer answer = saved.get(question.getId());
            ExamDtos.QuestionView item = new ExamDtos.QuestionView();
            item.setQuestionId(question.getId());
            item.setSortNo(link.getSortNo());
            item.setScore(link.getScore());
            item.setType(question.getType());
            item.setStem(question.getStem());
            item.setOptionsJson(question.getOptionsJson());
            item.setUserAnswer(answer == null ? "" : answer.getUserAnswer());
            if (showAnswer) {
                item.setStandardAnswer(question.getAnswer());
                item.setAnalysis(question.getAnalysis());
                item.setGotScore(answer == null ? 0 : answer.getScore());
                item.setCorrect(answer == null ? 0 : answer.getCorrect());
                item.setComment(answer == null ? null : answer.getComment());
            }
            items.add(item);
        }
        view.setQuestions(items);
        return view;
    }

    private Map<Long, ExamAnswer> answersOf(Long recordId) {
        return answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>().eq(ExamAnswer::getRecordId, recordId))
                .stream().collect(Collectors.toMap(ExamAnswer::getQuestionId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private ExamRecord own(Long recordId) {
        ExamRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BizException(404, "考试记录不存在");
        }
        if (!record.getUserId().equals(AuthUser.require().id()) && !staff()) {
            throw new BizException(403, "越权访问");
        }
        return record;
    }

    private ExamRecord lock(Long recordId) {
        ExamRecord record = recordMapper.lockById(recordId);
        if (record == null) {
            throw new BizException(404, "考试记录不存在");
        }
        if (!record.getUserId().equals(AuthUser.require().id()) && !staff()) {
            throw new BizException(403, "越权访问");
        }
        return record;
    }

    private void assertOngoing(ExamRecord record) {
        if (!record.getUserId().equals(AuthUser.require().id())) {
            throw new BizException(403, "越权访问");
        }
        if (!"ONGOING".equals(record.getStatus()) || !record.getDeadline().isAfter(LocalDateTime.now())) {
            throw new BizException("考试已结束，不能再修改答案");
        }
    }

    private boolean staff() {
        String role = AuthUser.require().role();
        return "ADMIN".equals(role) || "TEACHER".equals(role);
    }

    private String trim(String value) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        return text.length() > 2000 ? text.substring(0, 2000) : text;
    }
}
