package com.yunshu.mes.inventory.compat.service;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.repository.WmWarehouseRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WmWarehouseService {

    private final WmWarehouseRepository repo;

    public WmWarehouseService(WmWarehouseRepository repo) {
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

    public List<Map<String, Object>> getTreeList() {
        return repo.buildTree(repo.treeRows());
    }

    public Optional<Map<String, Object>> getById(Long id) {
        return repo.findById(id);
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        String code = String.valueOf(body.get("warehouseCode"));
        String name = String.valueOf(body.get("warehouseName"));
        if (repo.existsCode(code, null)) {
            throw new IllegalArgumentException("仓库编码已存在！");
        }
        if (repo.existsName(name, null)) {
            throw new IllegalArgumentException("仓库名称已存在！");
        }
        return repo.insert(body);
    }

    @Transactional
    public int update(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("warehouseId")));
        if (body.containsKey("warehouseCode") && repo.existsCode(String.valueOf(body.get("warehouseCode")), id)) {
            throw new IllegalArgumentException("仓库编码已存在！");
        }
        if (body.containsKey("warehouseName") && repo.existsName(String.valueOf(body.get("warehouseName")), id)) {
            throw new IllegalArgumentException("仓库名称已存在！");
        }
        if (body.containsKey("frozenFlag") && body.size() <= 3) {
            return repo.updateFrozenOnly(id, String.valueOf(body.get("frozenFlag")));
        }
        return repo.update(id, body);
    }

    @Transactional
    public int delete(Long id) {
        return repo.delete(id);
    }
}
