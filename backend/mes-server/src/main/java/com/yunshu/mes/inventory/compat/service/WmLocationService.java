package com.yunshu.mes.inventory.compat.service;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.repository.WmLocationRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WmLocationService {

    private final WmLocationRepository repo;

    public WmLocationService(WmLocationRepository repo) {
        this.repo = repo;
    }

    public List<Map<String, Object>> list(Map<String, String> params) {
        int pageNum = PageUtil.pageNum(params);
        int pageSize = PageUtil.pageSize(params);
        return repo.search(params, PageUtil.offset(pageNum, pageSize), pageSize);
    }

    public long count(Map<String, String> params) {
        return repo.count(params);
    }

    public Optional<Map<String, Object>> getById(Long id) {
        return repo.findById(id);
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        if (repo.existsCode(String.valueOf(body.get("locationCode")), null)) {
            throw new IllegalArgumentException("库区编码已存在!");
        }
        return repo.insert(body);
    }

    @Transactional
    public int update(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("locationId")));
        if (body.containsKey("locationCode") && repo.existsCode(String.valueOf(body.get("locationCode")), id)) {
            throw new IllegalArgumentException("库区编码已存在!");
        }
        return repo.update(id, body);
    }

    @Transactional
    public int delete(Long id) {
        return repo.delete(id);
    }

    @Transactional
    public void setProductMixing(Long locationId, boolean flag) {
        repo.setProductMixing(locationId, flag ? "Y" : "N");
    }

    @Transactional
    public void setBatchMixing(Long locationId, boolean flag) {
        repo.setBatchMixing(locationId, flag ? "Y" : "N");
    }
}
