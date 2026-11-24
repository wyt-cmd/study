package com.exam.category.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.category.entity.QuestionCategory;
import com.exam.category.mapper.CategoryMapper;
import com.exam.common.BizException;
import com.exam.question.entity.Question;
import com.exam.question.mapper.QuestionMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final QuestionMapper questionMapper;

    public List<Node> tree() {
        List<Map<String, Object>> rows = categoryMapper.listWithCount();
        Map<Long, Node> nodes = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Node node = new Node();
            node.setId(((Number) row.get("id")).longValue());
            node.setParentId(((Number) row.get("parentId")).longValue());
            node.setName(String.valueOf(row.get("name")));
            node.setSortNo(((Number) row.get("sortNo")).intValue());
            node.setQuestionCount(((Number) row.get("questionCount")).intValue());
            nodes.put(node.getId(), node);
        }
        List<Node> roots = new ArrayList<>();
        for (Node node : nodes.values()) {
            Node parent = nodes.get(node.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    public List<QuestionCategory> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<QuestionCategory>().orderByAsc(QuestionCategory::getSortNo));
    }

    @Transactional
    public void save(Long id, String name, Long parentId, Integer sortNo) {
        if (!StringUtils.hasText(name)) {
            throw new BizException("分类名称不能为空");
        }
        String trimmed = name.trim();
        LambdaQueryWrapper<QuestionCategory> dup = new LambdaQueryWrapper<QuestionCategory>().eq(QuestionCategory::getName, trimmed);
        if (id != null) {
            dup.ne(QuestionCategory::getId, id);
        }
        if (categoryMapper.selectCount(dup) > 0) {
            throw new BizException("分类名称已存在");
        }
        long parent = parentId == null ? 0L : parentId;
        if (parent != 0 && categoryMapper.selectById(parent) == null) {
            throw new BizException("父分类不存在");
        }
        if (id == null) {
            QuestionCategory category = new QuestionCategory();
            category.setName(trimmed);
            category.setParentId(parent);
            category.setSortNo(sortNo == null ? 0 : sortNo);
            categoryMapper.insert(category);
            return;
        }
        QuestionCategory category = require(id);
        if (parent == id) {
            throw new BizException("父分类不能是自己");
        }
        category.setName(trimmed);
        category.setParentId(parent);
        category.setSortNo(sortNo == null ? 0 : sortNo);
        categoryMapper.updateById(category);
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        long children = categoryMapper.selectCount(new LambdaQueryWrapper<QuestionCategory>().eq(QuestionCategory::getParentId, id));
        if (children > 0) {
            throw new BizException("请先删除子分类");
        }
        long questions = questionMapper.selectCount(new LambdaQueryWrapper<Question>().eq(Question::getCategoryId, id));
        if (questions > 0) {
            throw new BizException("分类下仍有题目，不能删除");
        }
        QuestionCategory category = require(id);
        category.setName(category.getName() + "#" + id);
        categoryMapper.updateById(category);
        categoryMapper.deleteById(id);
    }

    public QuestionCategory require(Long id) {
        QuestionCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(404, "分类不存在");
        }
        return category;
    }

    public QuestionCategory requireByName(String name) {
        QuestionCategory category = categoryMapper.selectOne(new LambdaQueryWrapper<QuestionCategory>().eq(QuestionCategory::getName, name));
        if (category == null) {
            throw new BizException("分类不存在: " + name);
        }
        return category;
    }

    @Data
    public static class Node {
        private Long id;
        private Long parentId;
        private String name;
        private Integer sortNo;
        private Integer questionCount;
        private List<Node> children = new ArrayList<>();
    }
}
