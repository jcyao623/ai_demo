package com.aidemo.service.node;

import com.aidemo.service.entity.AllocationLog;
import com.aidemo.service.mapper.AllocationLogMapper;
import com.aidemo.service.mapper.AllocationOrderMapper;
import com.aidemo.service.mapper.InventoryMapper;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.*;

@Component("executeNode")
public class ExecuteNode implements NodeAction {
    private static final Logger log = LoggerFactory.getLogger(ExecuteNode.class);
    private final InventoryMapper im; private final AllocationOrderMapper om; private final AllocationLogMapper lm;
    public ExecuteNode(InventoryMapper i, AllocationOrderMapper o, AllocationLogMapper l) { this.im = i; this.om = o; this.lm = l; }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) {
        String sku = state.value("sku",""); int qty = state.value("qty",0);
        log.info("[Node] 执行调拨: sku={}, qty={}", sku, qty);
        @SuppressWarnings("unchecked") List<Map<String,Object>> stock = (List<Map<String,Object>>)(List)state.value("stockInfo", new ArrayList<>());
        int alloc = 0;
        for (Map<String,Object> wh : stock) {
            int a = Math.min(((Number)wh.get("avail")).intValue(), qty - alloc);
            if (a <= 0) break;
            im.updateAllocatedQty(sku, (String)wh.get("code"), a); alloc += a;
        }
        om.updateStatus(state.value("orderNo",""), "COMPLETED", alloc);
        Map<String, Object> r = new LinkedHashMap<>(); r.put("allocated", alloc);
        try { AllocationLog l = new AllocationLog(); l.setOrderNo(state.value("orderNo","")); l.setStepName("执行调拨"); l.setStatus("OK"); l.setDetail("分配:" + alloc); l.setStartAt(LocalDateTime.now()); l.setEndAt(LocalDateTime.now()); lm.insertLog(l); } catch (Exception e) { log.warn("Log: {}", e.getMessage()); }
        return r;
    }
}