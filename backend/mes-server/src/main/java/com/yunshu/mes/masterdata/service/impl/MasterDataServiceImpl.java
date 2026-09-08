package com.yunshu.mes.masterdata.service.impl;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.masterdata.dto.BomItemRequest;
import com.yunshu.mes.masterdata.dto.BomRequest;
import com.yunshu.mes.masterdata.dto.MaterialRequest;
import com.yunshu.mes.masterdata.dto.ProductRequest;
import com.yunshu.mes.masterdata.dto.UomRequest;
import com.yunshu.mes.masterdata.repository.BomItemRepository;
import com.yunshu.mes.masterdata.repository.BomRepository;
import com.yunshu.mes.masterdata.repository.MaterialRepository;
import com.yunshu.mes.masterdata.repository.ProductRepository;
import com.yunshu.mes.masterdata.repository.ProductRouteRepository;
import com.yunshu.mes.masterdata.repository.UomRepository;
import com.yunshu.mes.masterdata.service.MasterDataMockDataService;
import com.yunshu.mes.masterdata.service.MasterDataService;
import com.yunshu.mes.masterdata.vo.BomItemVO;
import com.yunshu.mes.masterdata.vo.BomVO;
import com.yunshu.mes.masterdata.vo.MaterialVO;
import com.yunshu.mes.masterdata.vo.ProductVO;
import com.yunshu.mes.masterdata.vo.UomVO;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * 主数据域服务门面：优先 JDBC 仓储，异常时回退 Mock。
 */
@Service
@Primary
public class MasterDataServiceImpl implements MasterDataService {

    private static final Logger log = LoggerFactory.getLogger(MasterDataServiceImpl.class);

    private final ProductRepository productRepo;
    private final MaterialRepository materialRepo;
    private final BomRepository bomRepo;
    private final BomItemRepository bomItemRepo;
    private final ProductRouteRepository productRouteRepo;
    private final UomRepository uomRepo;
    private final MasterDataMockDataService mock;

    public MasterDataServiceImpl(ProductRepository productRepo, MaterialRepository materialRepo,
                                  BomRepository bomRepo, BomItemRepository bomItemRepo,
                                  ProductRouteRepository productRouteRepo,
                                  UomRepository uomRepo,
                                  MasterDataMockDataService mock) {
        this.productRepo = productRepo;
        this.materialRepo = materialRepo;
        this.bomRepo = bomRepo;
        this.bomItemRepo = bomItemRepo;
        this.productRouteRepo = productRouteRepo;
        this.uomRepo = uomRepo;
        this.mock = mock;
    }

    // ==================== 产品 ====================

    @Override
    public List<ProductVO> listProducts() {
        try { return productRepo.findAll(); }
        catch (DataAccessException e) { log.warn("产品列表回退 Mock：{}", e.getMessage()); return mock.listProducts(); }
    }

    @Override
    public Optional<ProductVO> getProductById(Long id) {
        try { return productRepo.findById(id).or(() -> mock.getProductById(id)); }
        catch (DataAccessException e) { log.warn("产品详情回退 Mock：{}", e.getMessage()); return mock.getProductById(id); }
    }

    @Override
    public ProductVO createProduct(ProductRequest req) {
        try {
            Long id = productRepo.insert(req.productCode(), req.productName(), req.productModel(),
                    req.category(), req.status());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建产品失败");
            return productRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建产品后回查失败"));
        } catch (DataAccessException e) { log.warn("创建产品回退 Mock：{}", e.getMessage()); return mock.createProduct(req); }
    }

    @Override
    public ProductVO updateProduct(Long id, ProductRequest req) {
        try {
            productRepo.update(id, req.productCode(), req.productName(), req.productModel(),
                    req.category(), req.status());
            return productRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "产品不存在"));
        } catch (DataAccessException e) { log.warn("更新产品回退 Mock：{}", e.getMessage()); return mock.updateProduct(id, req); }
    }

    @Override
    public void deleteProduct(Long id) {
        try { productRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除产品回退 Mock：{}", e.getMessage()); mock.deleteProduct(id); }
    }

    // ==================== 物料 ====================

    @Override
    public List<MaterialVO> listMaterials() {
        try { return materialRepo.findAll(); }
        catch (DataAccessException e) { log.warn("物料列表回退 Mock：{}", e.getMessage()); return mock.listMaterials(); }
    }

    @Override
    public Optional<MaterialVO> getMaterialById(Long id) {
        try { return materialRepo.findById(id).or(() -> mock.getMaterialById(id)); }
        catch (DataAccessException e) { log.warn("物料详情回退 Mock：{}", e.getMessage()); return mock.getMaterialById(id); }
    }

    @Override
    public MaterialVO createMaterial(MaterialRequest req) {
        try {
            Long id = materialRepo.insert(req.materialCode(), req.materialName(), req.materialType(),
                    req.unitId(), req.isCritical(), req.status());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建物料失败");
            return materialRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建物料后回查失败"));
        } catch (DataAccessException e) { log.warn("创建物料回退 Mock：{}", e.getMessage()); return mock.createMaterial(req); }
    }

    @Override
    public MaterialVO updateMaterial(Long id, MaterialRequest req) {
        try {
            materialRepo.update(id, req.materialCode(), req.materialName(), req.materialType(),
                    req.unitId(), req.isCritical(), req.status());
            return materialRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "物料不存在"));
        } catch (DataAccessException e) { log.warn("更新物料回退 Mock：{}", e.getMessage()); return mock.updateMaterial(id, req); }
    }

    @Override
    public void deleteMaterial(Long id) {
        try { materialRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除物料回退 Mock：{}", e.getMessage()); mock.deleteMaterial(id); }
    }

    // ==================== BOM ====================

    @Override
    public List<BomVO> listBoms() {
        try { return bomRepo.findAll(); }
        catch (DataAccessException e) { log.warn("BOM列表回退 Mock：{}", e.getMessage()); return mock.listBoms(); }
    }

    @Override
    public Optional<BomVO> getBomById(Long id) {
        try { return bomRepo.findById(id).or(() -> mock.getBomById(id)); }
        catch (DataAccessException e) { log.warn("BOM详情回退 Mock：{}", e.getMessage()); return mock.getBomById(id); }
    }

    @Override
    public BomVO createBom(BomRequest req) {
        try {
            Long id = bomRepo.insert(req.bomCode(), req.bomName(), req.productId(),
                    req.bomVersion(), req.status());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建BOM失败");
            return bomRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建BOM后回查失败"));
        } catch (DataAccessException e) { log.warn("创建BOM回退 Mock：{}", e.getMessage()); return mock.createBom(req); }
    }

    @Override
    public BomVO updateBom(Long id, BomRequest req) {
        try {
            bomRepo.update(id, req.bomCode(), req.bomName(), req.productId(),
                    req.bomVersion(), req.status());
            return bomRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "BOM不存在"));
        } catch (DataAccessException e) { log.warn("更新BOM回退 Mock：{}", e.getMessage()); return mock.updateBom(id, req); }
    }

    @Override
    public void deleteBom(Long id) {
        try { bomRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除BOM回退 Mock：{}", e.getMessage()); mock.deleteBom(id); }
    }

    // ==================== BOM 物料明细 ====================

    @Override
    public List<BomItemVO> listBomItems(Long bomId) {
        try { return bomItemRepo.findByBomId(bomId); }
        catch (DataAccessException e) { log.warn("BOM明细回退 Mock：{}", e.getMessage()); return List.of(); }
    }

    @Override
    public BomItemVO createBomItem(Long bomId, BomItemRequest req) {
        try {
            Long id = bomItemRepo.insert(bomId, req.materialId(), req.qtyPer(), req.lossRate(), req.isKeyMaterial(), req.remark());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建BOM明细失败");
            return bomItemRepo.findByBomId(bomId).stream().filter(i -> i.getBomItemId().equals(id)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "回查失败"));
        } catch (DataAccessException e) { log.warn("创建BOM明细回退 Mock：{}", e.getMessage()); throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage()); }
    }

    @Override
    public BomItemVO updateBomItem(Long itemId, BomItemRequest req) {
        try {
            bomItemRepo.update(itemId, req.materialId(), req.qtyPer(), req.lossRate(), req.isKeyMaterial(), req.remark());
            return bomItemRepo.findByBomId(req.bomId()).stream().filter(i -> i.getBomItemId().equals(itemId)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "BOM明细不存在"));
        } catch (DataAccessException e) { log.warn("更新BOM明细回退 Mock：{}", e.getMessage()); throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage()); }
    }

    @Override
    public void deleteBomItem(Long itemId) {
        try { bomItemRepo.delete(itemId); }
        catch (DataAccessException e) { log.warn("删除BOM明细回退 Mock：{}", e.getMessage()); }
    }

    // ==================== 产品-路线 ====================

    @Override
    public Long getProductRoute(Long productId) {
        try { return productRouteRepo.findRouteIdByProduct(productId); }
        catch (DataAccessException e) { log.warn("产品路线查询回退 Mock：{}", e.getMessage()); return null; }
    }

    @Override
    public void bindProductRoute(Long productId, Long routeId) {
        try { productRouteRepo.bind(productId, routeId); }
        catch (DataAccessException e) { log.warn("绑定产品路线回退 Mock：{}", e.getMessage()); }
    }

    // ==================== 计量单位 ====================

    @Override
    public List<UomVO> listUoms() {
        try { return uomRepo.findAll(); }
        catch (DataAccessException e) { log.warn("计量单位列表回退 Mock：{}", e.getMessage()); return mock.listUoms(); }
    }

    @Override
    public Optional<UomVO> getUomById(Long id) {
        try { return uomRepo.findById(id).or(() -> mock.getUomById(id)); }
        catch (DataAccessException e) { log.warn("计量单位详情回退 Mock：{}", e.getMessage()); return mock.getUomById(id); }
    }

    @Override
    public UomVO createUom(UomRequest req) {
        try {
            Long id = uomRepo.insert(req.unitCode(), req.unitName(), req.status());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建计量单位失败");
            return uomRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建计量单位后回查失败"));
        } catch (DataAccessException e) { log.warn("创建计量单位回退 Mock：{}", e.getMessage()); return mock.createUom(req); }
    }

    @Override
    public UomVO updateUom(Long id, UomRequest req) {
        try {
            uomRepo.update(id, req.unitCode(), req.unitName(), req.status());
            return uomRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "计量单位不存在"));
        } catch (DataAccessException e) { log.warn("更新计量单位回退 Mock：{}", e.getMessage()); return mock.updateUom(id, req); }
    }

    @Override
    public void deleteUom(Long id) {
        try { uomRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除计量单位回退 Mock：{}", e.getMessage()); mock.deleteUom(id); }
    }
}
