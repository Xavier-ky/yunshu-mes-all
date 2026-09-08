package com.yunshu.mes.planning.compat.service;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.masterdata.vo.ProductVO;
import com.yunshu.mes.planning.compat.dto.ClientOrderLineRequest;
import com.yunshu.mes.planning.compat.dto.ClientOrderSubmitRequest;
import com.yunshu.mes.planning.compat.repository.ClientCatalogRepository;
import com.yunshu.mes.planning.compat.vo.ClientCatalogItemVO;
import com.yunshu.mes.planning.compat.vo.ClientOrderLineResultVO;
import com.yunshu.mes.planning.compat.vo.ClientOrderResultVO;
import com.yunshu.mes.planning.repository.CustomerOrderItemRepository;
import com.yunshu.mes.planning.repository.CustomerOrderRepository;
import com.yunshu.mes.planning.vo.CustomerOrderVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ClientPortalService {

    public static final String FIXED_CUSTOMER_NAME = "软件2407隋若愚";

    private static final Map<String, String> CATEGORY_LABELS = Map.of(
            "FLOOR_FAN", "落地扇",
            "TABLE_FAN", "台扇",
            "WALL_FAN", "壁扇",
            "CEILING_FAN", "吊扇",
            "INDUSTRIAL_FAN", "工业扇",
            "BOX_FAN", "箱式扇",
            "TOWER_FAN", "塔扇",
            "HANDHELD_FAN", "手持扇"
    );

    /** 仅展示用虚拟 SKU，不写入 product 表 */
    public static final long VIRTUAL_CATALOG_PRODUCT_ID = 999999L;

    private static final Map<String, CatalogMeta> CATALOG_META = Map.ofEntries(
            Map.entry("FAN-FS40-A", new CatalogMeta("¥299", "三档风速 · 静音摇头 · 企业批量优选", "FLOOR_FAN")),
            Map.entry("FAN-TS30-B", new CatalogMeta("¥189", "台面条形底座 · 宿舍办公适用", "TABLE_FAN")),
            Map.entry("FAN-WS20-C", new CatalogMeta("¥159", "壁挂节省空间 · 厨房商铺通用", "WALL_FAN")),
            Map.entry("FAN-CF52-D", new CatalogMeta("¥459", "大直径吊扇 · 厂房车间通风", "CEILING_FAN")),
            Map.entry("FAN-IF60-E", new CatalogMeta("¥499", "工业级大风量 · 长时连续运行", "INDUSTRIAL_FAN")),
            Map.entry("FAN-BF16-F", new CatalogMeta("¥229", "便携箱式结构 · 多场景移动", "BOX_FAN")),
            Map.entry("FAN-TF35-G", new CatalogMeta("¥329", "塔式纤薄机身 · 立体送风", "TOWER_FAN")),
            Map.entry("FAN-HM28-H", new CatalogMeta("¥169", "无线便携 · 桌面手持两用", "HANDHELD_FAN"))
    );

    private final ClientCatalogRepository catalogRepo;
    private final CustomerOrderRepository orderRepo;
    private final CustomerOrderItemRepository orderItemRepo;

    public ClientPortalService(
            ClientCatalogRepository catalogRepo,
            CustomerOrderRepository orderRepo,
            CustomerOrderItemRepository orderItemRepo) {
        this.catalogRepo = catalogRepo;
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
    }

    public List<ClientCatalogItemVO> listCatalog() {
        List<ProductVO> products = catalogRepo.findEnabledProducts();
        Map<Long, List<String>> specsMap = catalogRepo.findSpecsByProduct();
        List<ClientCatalogItemVO> items = new ArrayList<>();
        for (ProductVO p : products) {
            CatalogMeta meta = CATALOG_META.getOrDefault(p.productCode(),
                    new CatalogMeta("¥199", "云枢智造标准风扇产品", p.category()));
            String category = p.category() != null ? p.category() : meta.category();
            items.add(new ClientCatalogItemVO(
                    p.productId(),
                    p.productCode(),
                    p.productName(),
                    p.productModel(),
                    category,
                    CATEGORY_LABELS.getOrDefault(category, "电风扇"),
                    specsMap.getOrDefault(p.productId(), List.of()),
                    "/images/client-catalog/" + p.productCode() + ".webp",
                    meta.displayPrice(),
                    meta.tagline()));
        }
        items.add(virtualCatalogItem());
        return items;
    }

    private ClientCatalogItemVO virtualCatalogItem() {
        CatalogMeta meta = CATALOG_META.get("FAN-HM28-H");
        return new ClientCatalogItemVO(
                VIRTUAL_CATALOG_PRODUCT_ID,
                "FAN-HM28-H",
                "28cm手持电风扇H型",
                "HM28-H",
                "HANDHELD_FAN",
                CATEGORY_LABELS.get("HANDHELD_FAN"),
                List.of("扇叶直径：28", "电池续航：8h", "颜色：天蓝"),
                "/images/client-catalog/FAN-HM28-H.webp",
                meta.displayPrice(),
                meta.tagline());
    }

    public ClientOrderResultVO submitOrder(ClientOrderSubmitRequest req) {
        List<ProductVO> products = catalogRepo.findEnabledProducts();
        Map<Long, ProductVO> productMap = products.stream()
                .collect(Collectors.toMap(ProductVO::productId, p -> p, (a, b) -> a, HashMap::new));

        List<ClientOrderLineResultVO> lineResults = new ArrayList<>();
        List<CustomerOrderRepository.OrderLineInsert> lines = new ArrayList<>();
        for (ClientOrderLineRequest line : req.items()) {
            ProductVO product = productMap.get(line.productId());
            if (product == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的产品 ID: " + line.productId());
            }
            lines.add(new CustomerOrderRepository.OrderLineInsert(line.productId(), line.orderQty()));
            lineResults.add(new ClientOrderLineResultVO(line.productId(), product.productName(), line.orderQty()));
        }

        String orderNo = "CO-CLIENT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        Long orderId = orderRepo.insertWithItems(
                orderNo,
                FIXED_CUSTOMER_NAME,
                req.deliveryDate(),
                "CREATED",
                req.remark(),
                lines);
        if (orderId == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建客户订单失败");
        }
        return new ClientOrderResultVO(orderId, orderNo, FIXED_CUSTOMER_NAME, req.deliveryDate(), "CREATED", lineResults);
    }

    public List<ClientOrderResultVO> listOrdersForClient() {
        return orderRepo.findByCustomerName(FIXED_CUSTOMER_NAME).stream()
                .map(this::toResult)
                .toList();
    }

    private ClientOrderResultVO toResult(CustomerOrderVO vo) {
        List<ClientOrderLineResultVO> items = orderItemRepo.findByOrderId(vo.orderId()).stream()
                .map(i -> new ClientOrderLineResultVO(i.productId(), i.productName(), i.orderQty()))
                .toList();
        return new ClientOrderResultVO(
                vo.orderId(), vo.orderNo(), vo.customerName(), vo.deliveryDate(), vo.status(), items);
    }

    private record CatalogMeta(String displayPrice, String tagline, String category) {
    }
}
