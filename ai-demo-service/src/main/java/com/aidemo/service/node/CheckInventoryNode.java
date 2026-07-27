package com.aidemo.service.node;

import com.aidemo.service.entity.AllocationLog;
import com.aidemo.service.mapper.AllocationLogMapper;
import com.aidemo.service.mapper.InventoryMapper;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component("checkInventoryNode")
public class CheckInventoryNode implements NodeAction {
    private static final Logger log = LoggerFactory.getLogger(CheckInventoryNode.class);
    private final InventoryMapper inventoryMapper; private final AllocationLogMapper logMapper;
    public CheckInventoryNode(InventoryMapper i, AllocationLogMapper l) { this.inventoryMapper = i; this.logMapper = l; }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String sku = state.value("sku", "");
        log.info("[Node] 库存检查: sku={}", sku);
        List<Map<String,Object>> stock = inventoryMapper.selectStockBySku(sku);
        int total = stock.stream().mapToInt(r -> ((Number)r.get("avail")).intValue()).sum();
        int req = state.value("qty", 0);
        if (total < req) throw new RuntimeException("库存不足: 需要" + req + ", 可用" + total);
        Map<String, Object> r = new LinkedHashMap<>(); r.put("stockInfo", stock); r.put("totalAvail", total);
        try { AllocationLog l = new AllocationLog(); l.setOrderNo(state.value("orderNo","")); l.setStepName("库存检查"); l.setStatus("OK"); l.setDetail("可用:" + total); l.setStartAt(LocalDateTime.now()); l.setEndAt(LocalDateTime.now()); logMapper.insertLog(l); } catch (Exception e) { log.warn("Log: {}", e.getMessage()); }
        return r;
    }
}