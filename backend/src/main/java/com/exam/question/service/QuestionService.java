package com.exam.question.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.ai.AiClient;
import com.exam.category.entity.QuestionCategory;
import com.exam.category.mapper.CategoryMapper;
import com.exam.category.service.CategoryService;
import com.exam.common.BizException;
import com.exam.common.ScoreRules;
import com.exam.common.StemHash;
import com.exam.question.dto.QuestionDtos;
import com.exam.question.entity.Question;
import com.exam.question.mapper.QuestionMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QuestionService extends ServiceImpl<QuestionMapper, Question> {

    private static final String HOT_KEY = "question:hot";
    private static final Set<String> TYPES = Set.of("SINGLE", "MULTI", "JUDGE", "SHORT");
    private static final DataFormatter FORMATTER = new DataFormatter();

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redis;
    private final AiClient aiClient;

    public Page<QuestionDtos.VO> page(long current, long size, Long categoryId, String type, Integer difficulty, String keyword) {
        Page<Question> page = new Page<>(current, size);
        Page<Question> raw = (Page<Question>) baseMapper.pageQuery(page, categoryId, type, difficulty, keyword);
        Map<Long, String> names = categoryNames();
        Page<QuestionDtos.VO> result = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        result.setRecords(raw.getRecords().stream().map(q -> toVo(q, names.get(q.getCategoryId()), true)).toList());
        return result;
    }

    public QuestionDtos.VO detail(Long id, boolean withAnswer) {
        Question question = require(id);
        String name = categoryService.require(question.getCategoryId()).getName();
        return toVo(question, name, withAnswer);
    }

    @Transactional
    public Long save(Long id, QuestionDtos.Save req) {
        Question question = id == null ? new Question() : require(id);
        fill(question, req);
        if (id == null) {
            save(question);
        } else {
            updateById(question);
        }
        redis.delete(HOT_KEY);
        return question.getId();
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        int rows = baseMapper.logicDeleteFreeHash(id);
        if (rows != 1) {
            throw new BizException("删除题目失败");
        }
        redis.delete(HOT_KEY);
    }

    public List<Map<String, Object>> hot() {
        String cached = redis.opsForValue().get(HOT_KEY);
        if (StringUtils.hasText(cached)) {
            try {
                return objectMapper.readValue(cached, new TypeReference<>() {
                });
            } catch (IOException ignored) {
                redis.delete(HOT_KEY);
            }
        }
        List<Map<String, Object>> rows = baseMapper.hot(10);
        try {
            redis.opsForValue().set(HOT_KEY, objectMapper.writeValueAsString(rows), Duration.ofMinutes(10));
        } catch (IOException ex) {
            throw new BizException("热门题目缓存失败");
        }
        return rows;
    }

    public byte[] template() {
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet("题目");
            Row head = sheet.createRow(0);
            String[] titles = {"题型", "难度", "分类名称", "分值", "题干", "选项A", "选项B", "选项C", "选项D", "答案", "解析", "关键词"};
            for (int i = 0; i < titles.length; i++) {
                head.createCell(i).setCellValue(titles[i]);
            }
            Row sample = sheet.createRow(1);
            String[] values = {"单选", "易", "Java基础", "5", "Java 中 int 是什么类型", "引用类型", "基本类型", "接口", "注解", "B", "int 是基本类型", ""};
            for (int i = 0; i < values.length; i++) {
                sample.createCell(i).setCellValue(values[i]);
            }
            book.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new BizException("生成模板失败");
        }
    }

    public QuestionDtos.ImportResult preview(MultipartFile file) {
        return readExcel(file, false);
    }

    @Transactional
    public QuestionDtos.ImportResult importExcel(MultipartFile file) {
        return readExcel(file, true);
    }

    public List<QuestionDtos.Save> aiDraft(QuestionDtos.AiRequest req) {
        if (req.getCount() < 1 || req.getCount() > 20) {
            throw new BizException("单次生成数量为 1 到 20");
        }
        categoryService.require(req.getCategoryId());
        String prompt = """
                你是出题教师。只返回 JSON 数组，不要 Markdown。
                每项字段：stem, options, answer, analysis, keywords。
                options 为 [{"key":"A","text":"..."}]，判断题 options 为空，答案用 T 或 F。
                多选答案用英文逗号分隔。主题：%s。题型：%s。数量：%d。难度 1 易 2 中 3 难，本次难度 %d。
                """.formatted(req.getTopic(), req.getType(), req.getCount(), req.getDifficulty());
        JsonNode node = aiClient.chat(prompt);
        if (!node.isArray()) {
            throw new BizException("模型返回的不是题目数组");
        }
        List<QuestionDtos.Save> drafts = new ArrayList<>();
        for (JsonNode item : node) {
            QuestionDtos.Save save = new QuestionDtos.Save();
            save.setCategoryId(req.getCategoryId());
            save.setType(req.getType());
            save.setDifficulty(req.getDifficulty());
            save.setScore(req.getScore());
            save.setStem(item.path("stem").asText(""));
            save.setAnswer(item.path("answer").asText(""));
            save.setAnalysis(item.path("analysis").asText(""));
            save.setKeywords(item.path("keywords").asText(""));
            List<QuestionDtos.Option> options = new ArrayList<>();
            for (JsonNode option : item.path("options")) {
                QuestionDtos.Option one = new QuestionDtos.Option();
                one.setKey(option.path("key").asText());
                one.setText(option.path("text").asText());
                options.add(one);
            }
            save.setOptions(options);
            drafts.add(save);
        }
        return drafts;
    }

    public Question require(Long id) {
        Question question = getById(id);
        if (question == null) {
            throw new BizException(404, "题目不存在");
        }
        return question;
    }

    private QuestionDtos.ImportResult readExcel(MultipartFile file, boolean persist) {
        long start = System.nanoTime();
        if (file == null || file.isEmpty()) {
            throw new BizException("请上传 Excel 文件");
        }
        List<Question> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Map<String, Long> categories = new LinkedHashMap<>();
        categoryMapper.selectList(null).forEach(c -> categories.put(c.getName(), c.getId()));
        try (InputStream in = file.getInputStream(); XSSFWorkbook book = new XSSFWorkbook(in)) {
            Sheet sheet = book.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || !StringUtils.hasText(text(row, 4))) {
                    continue;
                }
                try {
                    Question question = fromRow(row, categories);
                    if (!seen.add(question.getStemHash())) {
                        throw new BizException("文件内题干重复");
                    }
                    rows.add(question);
                } catch (BizException ex) {
                    errors.add("第 " + (i + 1) + " 行: " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            throw new BizException("读取 Excel 失败");
        }
        if (rows.isEmpty() && errors.isEmpty()) {
            throw new BizException("没有可导入的题目");
        }
        if (!rows.isEmpty()) {
            List<String> hashes = rows.stream().map(Question::getStemHash).toList();
            List<Question> exists = list(new LambdaQueryWrapper<Question>().in(Question::getStemHash, hashes));
            Set<String> duplicated = new HashSet<>();
            exists.forEach(q -> duplicated.add(q.getStemHash()));
            if (!duplicated.isEmpty()) {
                errors.add("与题库重复的题干有 " + duplicated.size() + " 条，已拦截");
                rows.removeIf(q -> duplicated.contains(q.getStemHash()));
            }
        }
        QuestionDtos.ImportResult result = new QuestionDtos.ImportResult();
        result.setErrors(errors);
        result.setInserted(rows.size());
        if (persist) {
            if (!errors.isEmpty()) {
                throw new BizException("导入被拦截：" + String.join("；", errors));
            }
            saveBatch(rows, 200);
            redis.delete(HOT_KEY);
        }
        result.setElapsedMs((System.nanoTime() - start) / 1_000_000);
        return result;
    }

    private Question fromRow(Row row, Map<String, Long> categories) {
        QuestionDtos.Save save = new QuestionDtos.Save();
        save.setType(mapType(text(row, 0)));
        save.setDifficulty(mapDifficulty(text(row, 1)));
        String category = text(row, 2);
        Long categoryId = categories.get(category);
        if (categoryId == null) {
            throw new BizException("分类不存在: " + category);
        }
        save.setCategoryId(categoryId);
        save.setScore(parseScore(text(row, 3)));
        save.setStem(text(row, 4));
        List<QuestionDtos.Option> options = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String text = text(row, 5 + i);
            if (StringUtils.hasText(text)) {
                QuestionDtos.Option option = new QuestionDtos.Option();
                option.setKey(String.valueOf((char) ('A' + i)));
                option.setText(text);
                options.add(option);
            }
        }
        save.setOptions(options);
        save.setAnswer(text(row, 9));
        save.setAnalysis(text(row, 10));
        save.setKeywords(text(row, 11));
        Question question = new Question();
        fill(question, save);
        return question;
    }

    private void fill(Question question, QuestionDtos.Save req) {
        if (!TYPES.contains(req.getType())) {
            throw new BizException("题型只能是 SINGLE、MULTI、JUDGE、SHORT");
        }
        if (req.getDifficulty() == null || req.getDifficulty() < 1 || req.getDifficulty() > 3) {
            throw new BizException("难度只能是 1、2、3");
        }
        if (req.getScore() == null || req.getScore() <= 0) {
            throw new BizException("分值必须为正整数");
        }
        categoryService.require(req.getCategoryId());
        String stem = StemHash.normalize(req.getStem());
        if (!StringUtils.hasText(stem)) {
            throw new BizException("题干不能为空");
        }
        String hash = StemHash.of(stem);
        LambdaQueryWrapper<Question> dup = new LambdaQueryWrapper<Question>().eq(Question::getStemHash, hash);
        if (question.getId() != null) {
            dup.ne(Question::getId, question.getId());
        }
        if (count(dup) > 0) {
            throw new BizException("题干重复");
        }
        String answer = ScoreRules.normalizeObjective(req.getType(), req.getAnswer());
        if (!StringUtils.hasText(answer)) {
            throw new BizException("答案不能为空");
        }
        question.setCategoryId(req.getCategoryId());
        question.setType(req.getType());
        question.setDifficulty(req.getDifficulty());
        question.setStem(stem);
        question.setStemHash(hash);
        question.setAnswer(answer);
        question.setAnalysis(req.getAnalysis());
        question.setKeywords(req.getKeywords());
        question.setScore(req.getScore());
        question.setImageUrl(req.getImageUrl());
        try {
            if ("JUDGE".equals(req.getType())) {
                question.setOptionsJson(objectMapper.writeValueAsString(List.of(
                        Map.of("key", "T", "text", "正确"),
                        Map.of("key", "F", "text", "错误")
                )));
            } else if ("SHORT".equals(req.getType())) {
                question.setOptionsJson(null);
            } else {
                if (req.getOptions() == null || req.getOptions().size() < 2) {
                    throw new BizException("选择题至少两个选项");
                }
                question.setOptionsJson(objectMapper.writeValueAsString(req.getOptions()));
            }
        } catch (IOException ex) {
            throw new BizException("选项序列化失败");
        }
    }

    private QuestionDtos.VO toVo(Question question, String categoryName, boolean withAnswer) {
        QuestionDtos.VO vo = new QuestionDtos.VO();
        vo.setId(question.getId());
        vo.setCategoryId(question.getCategoryId());
        vo.setCategoryName(categoryName);
        vo.setType(question.getType());
        vo.setDifficulty(question.getDifficulty());
        vo.setStem(question.getStem());
        vo.setScore(question.getScore());
        vo.setImageUrl(question.getImageUrl());
        vo.setCreateTime(question.getCreateTime());
        if (StringUtils.hasText(question.getOptionsJson())) {
            try {
                vo.setOptions(objectMapper.readValue(question.getOptionsJson(), new TypeReference<>() {
                }));
            } catch (IOException ex) {
                vo.setOptions(List.of());
            }
        }
        if (withAnswer) {
            vo.setAnswer(question.getAnswer());
            vo.setAnalysis(question.getAnalysis());
            vo.setKeywords(question.getKeywords());
        }
        return vo;
    }

    private Map<Long, String> categoryNames() {
        Map<Long, String> names = new LinkedHashMap<>();
        for (QuestionCategory category : categoryMapper.selectList(null)) {
            names.put(category.getId(), category.getName());
        }
        return names;
    }

    private static String text(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return "";
        }
        return FORMATTER.formatCellValue(cell).trim();
    }

    private static int parseScore(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BizException("分值不能为空");
        }
        try {
            BigDecimal number = new BigDecimal(raw);
            if (number.stripTrailingZeros().scale() > 0) {
                throw new BizException("分值必须为整数");
            }
            int score = number.intValueExact();
            if (score <= 0) {
                throw new BizException("分值必须为正整数");
            }
            return score;
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new BizException("分值必须为整数");
        }
    }

    private static String mapType(String raw) {
        return switch (raw) {
            case "单选", "SINGLE" -> "SINGLE";
            case "多选", "MULTI" -> "MULTI";
            case "判断", "JUDGE" -> "JUDGE";
            case "简答", "SHORT" -> "SHORT";
            default -> throw new BizException("无法识别的题型: " + raw);
        };
    }

    private static int mapDifficulty(String raw) {
        return switch (raw) {
            case "易", "1" -> 1;
            case "中", "2" -> 2;
            case "难", "3" -> 3;
            default -> throw new BizException("无法识别的难度: " + raw);
        };
    }
}
