package com.yunshu.mes.inventory.compat.service;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.repository.WmStockRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WmStockService {

    private final WmStockRepository repo;

    public WmStockService(WmStockRepository repo) {
        this.repo = repo;
    }

    public List<Map<String, Object>> list(Map<String, String> params) {
        int pageNum = PageUtil.pageNum(params);
        int pageSize = PageUtil.pageSize(params);
        return repo.search(params, PageUtil.offset(pageNum, pageSize), pageSize);
    }

    public List<Map<String, Object>> listAll(Map<String, String> params, int maxRows) {
        return repo.search(params, 0, maxRows);
    }

    public long count(Map<String, String> params) {
        return repo.count(params);
    }

    public Map<String, Object> overview() {
        return repo.overview();
    }

    public Optional<Map<String, Object>> binMap(Map<String, String> params) {
        return repo.binMap(params);
    }

    public Optional<Map<String, Object>> getById(Long id) {
        return repo.findById(id);
    }

    public Optional<Map<String, Object>> getBatchById(Long batchId) {
        return repo.findBatchById(batchId);
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        return repo.insert(body);
    }

    @Transactional
    public int update(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("materialStockId")));
        return repo.update(id, body);
    }

    @Transactional
    public int delete(Long id) {
        return repo.delete(id);
    }
}
