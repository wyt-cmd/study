package com.exam.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.record.entity.ExamRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {

    @Select("SELECT * FROM exam_record WHERE id = #{id} FOR UPDATE")
    ExamRecord lockById(@Param("id") Long id);

    @Update("""
            UPDATE exam_record
            SET status = 'TIMEOUT', score = 0, objective_score = 0, subjective_score = 0, submit_time = deadline
            WHERE status = 'ONGOING' AND deadline < NOW()
            """)
    int closeExpired();

    IPage<Map<String, Object>> pageRecords(Page<Map<String, Object>> page,
                                           @Param("paperId") Long paperId,
                                           @Param("userId") Long userId);

    List<Map<String, Object>> rank(@Param("paperId") Long paperId);

    List<Map<String, Object>> aggregate();
}
