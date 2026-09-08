package com.yunshu.mes.inventory.compat.service;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.repository.WmAreaRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WmAreaService {

    private final WmAreaRepository repo;

    public WmAreaService(WmAreaRepository repo) {
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
        if (repo.existsCode(String.valueOf(body.get("areaCode")), null)) {
            throw new IllegalArgumentException("库位编码已存在!");
        }
        return repo.insert(body);
    }

    @Transactional
    public int update(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("areaId")));
        if (body.containsKey("areaCode") && repo.existsCode(String.valueOf(body.get("areaCode")), id)) {
            throw new IllegalArgumentException("库位编码已存在!");
        }
        return repo.update(id, body);
    }

    @Transactional
    public int delete(Long id) {
        return repo.delete(id);
    }

    public Map<String, Object> getLocationName(Long areaId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("locationName", repo.resolveLocationName(areaId).orElse(""));
        return m;
    }
}
