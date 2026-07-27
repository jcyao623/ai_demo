package com.aidemo.service.service;

import com.aidemo.service.entity.AllocationOrder;
import com.aidemo.service.mapper.AllocationOrderMapper;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class WmsAllocationService {
    private static final Logger log = LoggerFactory.getLogger(WmsAllocationService.class);
    private final StateGraph wmsGraph;
    private final AllocationOrderMapper orderMapper;

    public WmsAllocationService(StateGraph wmsGraph, AllocationOrderMapper orderMapper) {
        this.wmsGraph = wmsGraph;
        this.orderMapper = orderMapper;
    }

    public Map<String,Object> allocate(String orderNo, String sku, String pn, int qty) {
        log.info("[WMS] 启动调拨: orderNo={}, sku={}, qty={}", orderNo, sku, qty);
        try {
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("orderNo", orderNo); input.put("sku", sku);
            input.put("productName", pn); input.put("qty", qty);
            
            CompiledGraph compiled = wmsGraph.compile();
            Optional<OverAllState> opt = compiled.call(input);
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("overall", "SUCCESS");
            result.put("orderNo", orderNo);
            opt.ifPresent(s -> {
                s.value("orderId").ifPresent(v -> result.put("orderId", v));
                s.value("allocated").ifPresent(v -> result.put("allocated", v));
            });
            log.info("[WMS] 调拨完成: orderNo={}", orderNo);
            return result;
        } catch (Exception e) {
            log.error("[WMS] 调拨失败: {}", e.getMessage());
            Map<String,Object> err = new LinkedHashMap<>();
            err.put("overall", "FAILED"); err.put("orderNo", orderNo);
            err.put("error", e.getMessage()); return err;
        }
    }

    public Map<String,Object> getOrder(String orderNo) {
        var o = orderMapper.selectOne(new LambdaQueryWrapper<AllocationOrder>().eq(AllocationOrder::getOrderNo, orderNo));
        if (o == null) return Map.of("error","not found");
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("orderNo",o.getOrderNo()); m.put("sku",o.getSku());
        m.put("requiredQty",o.getRequiredQty()); m.put("allocatedQty",o.getAllocatedQty());
        m.put("status",o.getStatus()); return m;
    }

    public Map<String,Object> structure() {
        Map<String,Object> s = new LinkedHashMap<>();
        s.put("framework","Spring AI Alibaba StateGraph");
        s.put("states",List.of("create-order","check-inventory","ai-decide","execute","complete"));
        s.put("edges",List.of(
            Map.of("from","create-order","to","check-inventory"),
            Map.of("from","check-inventory","to","ai-decide"),
            Map.of("from","ai-decide","to","execute"),
            Map.of("from","execute","to","complete")
        ));
        return s;
    }
}