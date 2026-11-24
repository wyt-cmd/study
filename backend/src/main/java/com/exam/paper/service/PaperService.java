package com.exam.paper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.BizException;
import com.exam.paper.PaperPicker;
import com.exam.paper.dto.PaperDtos;
import com.exam.paper.entity.Paper;
import com.exam.paper.entity.PaperQuestion;
import com.exam.paper.entity.PaperRule;
import com.exam.paper.mapper.PaperMapper;
import com.exam.paper.mapper.PaperQuestionMapper;
import com.exam.paper.mapper.PaperRuleMapper;
import com.exam.question.entity.Question;
import com.exam.question.mapper.QuestionMapper;
import com.exam.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaperService {

    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final PaperRuleMapper paperRuleMapper;
    private final QuestionMapper questionMapper;

    public Page<Paper> page(long current, long size, String status, String keyword, boolean publishedOnly) {
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<Paper>()
                .eq(publishedOnly, Paper::getStatus, "PUBLISHED")
                .eq(!publishedOnly && StringUtils.hasText(status), Paper::getStatus, status)
                .like(StringUtils.hasText(keyword), Paper::getTitle, keyword)
                .orderByDesc(Paper::getCreateTime);
        return paperMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public PaperDtos.PaperVO detail(Long id, boolean withAnswer) {
        Paper paper = require(id);
        PaperDtos.PaperVO vo = toVo(paper);
        List<PaperQuestion> links = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, id).orderByAsc(PaperQuestion::getSortNo));
        Map<Long, Question> questions = links.isEmpty() ? Map.of() : questionMapper.selectBatchIds(
                links.stream().map(PaperQuestion::getQuestionId).toList()
        ).stream().collect(Collectors.toMap(Question::getId, Function.identity()));
        List<PaperDtos.QuestionItem> items = new ArrayList<>();
        for (PaperQuestion link : links) {
            Question question = questions.get(link.getQuestionId());
            if (question == null) {
                continue;
            }
            PaperDtos.QuestionItem item = new PaperDtos.QuestionItem();
            item.setQuestionId(question.getId());
            item.setScore(link.getScore());
            item.setSortNo(link.getSortNo());
            item.setStem(question.getStem());
            item.setType(question.getType());
            item.setOptionsJson(question.getOptionsJson());
            if (withAnswer) {
                item.setAnswer(question.getAnswer());
                item.setAnalysis(question.getAnalysis());
                item.setKeywords(question.getKeywords());
            }
            items.add(item);
        }
        vo.setQuestions(items);
        List<PaperRule> rules = paperRuleMapper.selectList(new LambdaQueryWrapper<PaperRule>().eq(PaperRule::getPaperId, id));
        vo.setRules(rules.stream().map(rule -> {
            PaperDtos.RuleItem item = new PaperDtos.RuleItem();
            item.setCategoryId(rule.getCategoryId());
            item.setType(rule.getType());
            item.setCount(rule.getQuestionCount());
            item.setScoreEach(rule.getScoreEach());
            return item;
        }).toList());
        return vo;
    }

    @Transactional
    public Long manual(PaperDtos.ManualCreate req) {
        checkCommon(req.getDurationMin(), req.getTotalScore(), req.getPassScore());
        List<PaperPicker.Picked> picked = req.getQuestions().stream()
                .map(item -> new PaperPicker.Picked(item.getQuestionId(), item.getScore()))
                .toList();
        PaperPicker.checkManual(req.getTotalScore(), picked);
        List<Question> questions = questionMapper.selectBatchIds(picked.stream().map(PaperPicker.Picked::questionId).toList());
        if (questions.size() != picked.size()) {
            throw new BizException("包含不存在的题目");
        }
        Paper paper = newPaper(req.getTitle(), req.getDescription(), req.getDurationMin(), req.getTotalScore(), req.getPassScore(), "MANUAL");
        paperMapper.insert(paper);
        insertLinks(paper.getId(), picked);
        return paper.getId();
    }

    @Transactional
    public Long smart(PaperDtos.SmartCreate req) {
        if (req.getDurationMin() == null || req.getDurationMin() <= 0 || req.getDurationMin() > 300) {
            throw new BizException("考试时长须在 1 到 300 分钟");
        }
        List<PaperPicker.Rule> rules = req.getRules().stream()
                .map(item -> new PaperPicker.Rule(item.getCategoryId(), item.getType(), item.getCount(), item.getScoreEach()))
                .toList();
        int total = rules.stream().mapToInt(rule -> rule.count() * rule.scoreEach()).sum();
        if (req.getPassScore() == null || req.getPassScore() < 0 || req.getPassScore() > total) {
            throw new BizException("及格分必须介于 0 和总分之间");
        }
        List<List<Long>> pools = new ArrayList<>();
        for (PaperPicker.Rule rule : rules) {
            pools.add(questionMapper.listIds(rule.categoryId(), rule.type()));
        }
        List<PaperPicker.Picked> picked = PaperPicker.pick(rules, pools, total, new Random());
        Paper paper = newPaper(req.getTitle(), req.getDescription(), req.getDurationMin(), total, req.getPassScore(), "SMART");
        paperMapper.insert(paper);
        insertLinks(paper.getId(), picked);
        for (PaperDtos.RuleItem item : req.getRules()) {
            PaperRule rule = new PaperRule();
            rule.setPaperId(paper.getId());
            rule.setCategoryId(item.getCategoryId());
            rule.setType(item.getType());
            rule.setQuestionCount(item.getCount());
            rule.setScoreEach(item.getScoreEach());
            paperRuleMapper.insert(rule);
        }
        return paper.getId();
    }

    @Transactional
    public void changeStatus(Long id, String status) {
        if (!List.of("DRAFT", "PUBLISHED", "CLOSED").contains(status)) {
            throw new BizException("状态不合法");
        }
        Paper paper = require(id);
        paper.setStatus(status);
        paperMapper.updateById(paper);
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        paperMapper.deleteById(id);
    }

    public Paper require(Long id) {
        Paper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw new BizException(404, "试卷不存在");
        }
        return paper;
    }

    public List<PaperQuestion> links(Long paperId) {
        return paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paperId)
                .orderByAsc(PaperQuestion::getSortNo));
    }

    private void insertLinks(Long paperId, List<PaperPicker.Picked> picked) {
        int sort = 1;
        for (PaperPicker.Picked item : picked) {
            PaperQuestion link = new PaperQuestion();
            link.setPaperId(paperId);
            link.setQuestionId(item.questionId());
            link.setScore(item.score());
            link.setSortNo(sort++);
            paperQuestionMapper.insert(link);
        }
    }

    private Paper newPaper(String title, String description, int duration, int total, int pass, String mode) {
        Paper paper = new Paper();
        paper.setTitle(title.trim());
        paper.setDescription(description);
        paper.setDurationMin(duration);
        paper.setTotalScore(total);
        paper.setPassScore(pass);
        paper.setStatus("DRAFT");
        paper.setAssembleMode(mode);
        paper.setCreatorId(AuthUser.require().id());
        return paper;
    }

    private void checkCommon(Integer duration, Integer total, Integer pass) {
        if (duration == null || duration <= 0 || duration > 300) {
            throw new BizException("考试时长须在 1 到 300 分钟");
        }
        if (total == null || total <= 0) {
            throw new BizException("总分必须为正整数");
        }
        if (pass == null || pass < 0 || pass > total) {
            throw new BizException("及格分必须介于 0 和总分之间");
        }
    }

    private PaperDtos.PaperVO toVo(Paper paper) {
        PaperDtos.PaperVO vo = new PaperDtos.PaperVO();
        vo.setId(paper.getId());
        vo.setTitle(paper.getTitle());
        vo.setDescription(paper.getDescription());
        vo.setDurationMin(paper.getDurationMin());
        vo.setTotalScore(paper.getTotalScore());
        vo.setPassScore(paper.getPassScore());
        vo.setStatus(paper.getStatus());
        vo.setAssembleMode(paper.getAssembleMode());
        vo.setCreateTime(paper.getCreateTime());
        return vo;
    }
}
