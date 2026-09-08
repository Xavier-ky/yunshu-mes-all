package com.yunshu.mes.masterdata.service;

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
import java.util.List;
import java.util.Optional;

/**
 * 主数据域服务接口：产品、物料、BOM、计量单位。
 */
public interface MasterDataService {

    // ---- 产品 ----
    List<ProductVO> listProducts();

    Optional<ProductVO> getProductById(Long id);

    ProductVO createProduct(ProductRequest req);

    ProductVO updateProduct(Long id, ProductRequest req);

    void deleteProduct(Long id);

    // ---- 物料 ----
    List<MaterialVO> listMaterials();

    Optional<MaterialVO> getMaterialById(Long id);

    MaterialVO createMaterial(MaterialRequest req);

    MaterialVO updateMaterial(Long id, MaterialRequest req);

    void deleteMaterial(Long id);

    // ---- BOM ----
    List<BomVO> listBoms();

    Optional<BomVO> getBomById(Long id);

    BomVO createBom(BomRequest req);

    BomVO updateBom(Long id, BomRequest req);

    void deleteBom(Long id);

    // ---- BOM 物料明细 ----
    List<BomItemVO> listBomItems(Long bomId);
    BomItemVO createBomItem(Long bomId, BomItemRequest req);
    BomItemVO updateBomItem(Long itemId, BomItemRequest req);
    void deleteBomItem(Long itemId);

    // ---- 产品-路线 ----
    Long getProductRoute(Long productId);
    void bindProductRoute(Long productId, Long routeId);

    // ---- 计量单位 ----
    List<UomVO> listUoms();

    Optional<UomVO> getUomById(Long id);

    UomVO createUom(UomRequest req);

    UomVO updateUom(Long id, UomRequest req);

    void deleteUom(Long id);
}
