package com.exam.banner.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.banner.entity.Banner;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BannerMapper extends BaseMapper<Banner> {
}
