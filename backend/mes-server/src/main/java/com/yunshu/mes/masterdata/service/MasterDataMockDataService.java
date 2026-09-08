package com.yunshu.mes.masterdata.service;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.masterdata.dto.BomItemRequest;
import com.yunshu.mes.masterdata.dto.BomRequest;
import com.yunshu.mes.masterdata.dto.MaterialRequest;
import com.yunshu.mes.masterdata.dto.ProductRequest;
import com.yunshu.mes.masterdata.dto.UomRequest;
import com.yunshu.mes.masterdata.vo.BomItemVO;
import com.yunshu.mes.masterdata.vo.BomVO;
import com.yunshu.mes.masterdata.vo.MaterialVO;
import com.yunshu.mes.masterdata.vo.ProductVO;
import com.yunshu.mes.masterdata.vo.UomVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/**
 * 主数据域 Mock 实现，写操作在无库时不可用。
 */
@Service
public class MasterDataMockDataService implements MasterDataService {

    private static final List<ProductVO> PRODUCTS = new ArrayList<>(List.of(
            new ProductVO(1L, "FAN-001", "台扇 TS-300", "TS-300", "台扇", "ENABLED"),
            new ProductVO(2L, "FAN-002", "落地扇 FS-500", "FS-500", "落地扇", "ENABLED"),
            new ProductVO(3L, "FAN-003", "壁扇 WS-200", "WS-200", "壁扇", "ENABLED")
    ));

    private static final List<MaterialVO> MATERIALS = new ArrayList<>(List.of(
            new MaterialVO(1L, "MAT-001", "交流电机 60W", "RAW", "台", "1", "ENABLED"),
            new MaterialVO(2L, "MAT-002", "扇叶 16寸", "RAW", "片", "1", "ENABLED"),
            new MaterialVO(3L, "MAT-003", "底座总成", "SEMI", "个", "0", "ENABLED"),
            new MaterialVO(4L, "MAT-004", "电源线 1.5m", "RAW", "根", "0", "ENABLED")
    ));

    private static final List<BomVO> BOMS = new ArrayList<>(List.of(
            new BomVO(1L, "BOM-001", "台扇 TS-300 标准BOM", "台扇 TS-300", "V1.0", "ENABLED"),
            new BomVO(2L, "BOM-002", "落地扇 FS-500 标准BOM", "落地扇 FS-500", "V1.0", "ENABLED"),
            new BomVO(3L, "BOM-003", "壁扇 WS-200 标准BOM", "壁扇 WS-200", "V1.0", "ENABLED")
    ));

    private static final List<UomVO> UOMS = new ArrayList<>(List.of(
            new UomVO(1L, "PCS", "个", "ENABLED"),
            new UomVO(2L, "BOX", "箱", "ENABLED"),
            new UomVO(3L, "KG", "千克", "ENABLED"),
            new UomVO(4L, "M", "米", "ENABLED")
    ));

    private final AtomicLong productNextId = new AtomicLong(100);
    private final AtomicLong materialNextId = new AtomicLong(100);
    private final AtomicLong bomNextId = new AtomicLong(100);
    private final AtomicLong uomNextId = new AtomicLong(100);

    // ---- 产品 ----
    @Override
    public List<ProductVO> listProducts() {
        return new ArrayList<>(PRODUCTS);
    }

    @Override
    public Optional<ProductVO> getProductById(Long id) {
        return PRODUCTS.stream().filter(p -> p.productId().equals(id)).findFirst();
    }

    @Override
    public ProductVO createProduct(ProductRequest req) {
        throw mockWriteError();
    }

    @Override
    public ProductVO updateProduct(Long id, ProductRequest req) {
        throw mockWriteError();
    }

    @Override
    public void deleteProduct(Long id) {
        throw mockWriteError();
    }

    // ---- 物料 ----
    @Override
    public List<MaterialVO> listMaterials() {
        return new ArrayList<>(MATERIALS);
    }

    @Override
    public Optional<MaterialVO> getMaterialById(Long id) {
        return MATERIALS.stream().filter(m -> m.materialId().equals(id)).findFirst();
    }

    @Override
    public MaterialVO createMaterial(MaterialRequest req) {
        throw mockWriteError();
    }

    @Override
    public MaterialVO updateMaterial(Long id, MaterialRequest req) {
        throw mockWriteError();
    }

    @Override
    public void deleteMaterial(Long id) {
        throw mockWriteError();
    }

    // ---- BOM ----
    @Override
    public List<BomVO> listBoms() {
        return new ArrayList<>(BOMS);
    }

    @Override
    public Optional<BomVO> getBomById(Long id) {
        return BOMS.stream().filter(b -> b.bomId().equals(id)).findFirst();
    }

    @Override
    public BomVO createBom(BomRequest req) {
        throw mockWriteError();
    }

    @Override
    public BomVO updateBom(Long id, BomRequest req) {
        throw mockWriteError();
    }

    @Override
    public void deleteBom(Long id) {
        throw mockWriteError();
    }

    @Override public List<BomItemVO> listBomItems(Long bomId) { return List.of(); }
    @Override public BomItemVO createBomItem(Long bomId, BomItemRequest req) { throw mockWriteError(); }
    @Override public BomItemVO updateBomItem(Long itemId, BomItemRequest req) { throw mockWriteError(); }
    @Override public void deleteBomItem(Long itemId) { throw mockWriteError(); }
    @Override public Long getProductRoute(Long productId) { return null; }
    @Override public void bindProductRoute(Long productId, Long routeId) { throw mockWriteError(); }

    // ---- 计量单位 ----
    @Override
    public List<UomVO> listUoms() {
        return new ArrayList<>(UOMS);
    }

    @Override
    public Optional<UomVO> getUomById(Long id) {
        return UOMS.stream().filter(u -> u.unitId().equals(id)).findFirst();
    }

    @Override
    public UomVO createUom(UomRequest req) {
        throw mockWriteError();
    }

    @Override
    public UomVO updateUom(Long id, UomRequest req) {
        throw mockWriteError();
    }

    @Override
    public void deleteUom(Long id) {
        throw mockWriteError();
    }

    private BusinessException mockWriteError() {
        return new BusinessException(ErrorCode.BAD_REQUEST,
                "写操作需连接 MySQL 且 fan_mes 库已执行迁移脚本，当前为 Mock 模式");
    }
}
