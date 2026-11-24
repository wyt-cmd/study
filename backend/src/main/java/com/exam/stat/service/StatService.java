package com.exam.stat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.BizException;
import com.exam.record.mapper.ExamRecordMapper;
import com.exam.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatService {

    private final ExamRecordMapper recordMapper;

    public Page<Map<String, Object>> records(long current, long size, Long paperId, boolean mineOnly) {
        Long userId = mineOnly ? AuthUser.require().id() : null;
        Page<Map<String, Object>> page = new Page<>(current, size);
        return (Page<Map<String, Object>>) recordMapper.pageRecords(page, paperId, userId);
    }

    public List<Map<String, Object>> aggregate() {
        List<Map<String, Object>> rows = recordMapper.aggregate();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>(row);
            long attend = number(row.get("attendCount"));
            long scoreSum = number(row.get("scoreSum"));
            long pass = number(row.get("passCount"));
            item.put("avgScore", attend == 0 ? 0 : (scoreSum + attend / 2) / attend);
            item.put("passRate", attend == 0 ? 0 : pass * 100 / attend);
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> rank(Long paperId) {
        if (paperId == null) {
            throw new BizException("请选择试卷");
        }
        return recordMapper.rank(paperId);
    }

    public byte[] export(Long paperId) {
        Page<Map<String, Object>> page = new Page<>(1, 5000, false);
        List<Map<String, Object>> rows = recordMapper.pageRecords(page, paperId, null).getRecords();
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet("成绩");
            Row head = sheet.createRow(0);
            String[] titles = {"试卷", "考生", "状态", "得分", "客观分", "主观分", "总分", "及格分", "开考时间", "交卷时间"};
            for (int i = 0; i < titles.length; i++) {
                head.createCell(i).setCellValue(titles[i]);
            }
            int line = 1;
            for (Map<String, Object> row : rows) {
                Row excel = sheet.createRow(line++);
                excel.createCell(0).setCellValue(text(row.get("paperTitle")));
                excel.createCell(1).setCellValue(text(row.get("nickname")));
                excel.createCell(2).setCellValue(text(row.get("status")));
                excel.createCell(3).setCellValue(text(row.get("score")));
                excel.createCell(4).setCellValue(text(row.get("objectiveScore")));
                excel.createCell(5).setCellValue(text(row.get("subjectiveScore")));
                excel.createCell(6).setCellValue(text(row.get("totalScore")));
                excel.createCell(7).setCellValue(text(row.get("passScore")));
                excel.createCell(8).setCellValue(text(row.get("startTime")));
                excel.createCell(9).setCellValue(text(row.get("submitTime")));
            }
            book.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new BizException("导出成绩失败");
        }
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
