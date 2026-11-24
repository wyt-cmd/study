package com.exam.banner.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.banner.entity.Banner;
import com.exam.banner.mapper.BannerMapper;
import com.exam.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerMapper bannerMapper;

    public List<Banner> active() {
        return bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getStatus, 1)
                .orderByAsc(Banner::getSortNo)
                .orderByDesc(Banner::getId));
    }

    public List<Banner> list() {
        return bannerMapper.selectList(new LambdaQueryWrapper<Banner>().orderByAsc(Banner::getSortNo));
    }

    @Transactional
    public Long save(Long id, String title, String imageUrl, String linkUrl, Integer sortNo, Integer status) {
        if (!StringUtils.hasText(title) || !StringUtils.hasText(imageUrl)) {
            throw new BizException("标题和图片不能为空");
        }
        Banner banner = id == null ? new Banner() : require(id);
        banner.setTitle(title.trim());
        banner.setImageUrl(imageUrl.trim());
        banner.setLinkUrl(linkUrl);
        banner.setSortNo(sortNo == null ? 0 : sortNo);
        banner.setStatus(status != null && status == 0 ? 0 : 1);
        if (id == null) {
            bannerMapper.insert(banner);
        } else {
            bannerMapper.updateById(banner);
        }
        return banner.getId();
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        bannerMapper.deleteById(id);
    }

    private Banner require(Long id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BizException(404, "轮播不存在");
        }
        return banner;
    }
}
