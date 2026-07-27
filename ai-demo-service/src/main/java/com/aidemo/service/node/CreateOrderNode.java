package com.aidemo.service.node;

import com.aidemo.service.entity.AllocationLog;
import com.aidemo.service.entity.AllocationOrder;
import com.aidemo.service.mapper.AllocationLogMapper;
import com.aidemo.service.mapper.AllocationOrderMapper;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component("createOrderNode")
public class CreateOrderNode implements NodeAction {
    private static final Logger log = LoggerFactory.getLogger(CreateOrderNode.class);
    private final AllocationOrderMapper orderMapper;
    private final AllocationLogMapper logMapper;
    public CreateOrderNode(AllocationOrderMapper o, AllocationLogMapper l) { this.orderMapper = o; this.logMapper = l; }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String orderNo = state.value("orderNo", "");
        log.info("[Node] 创建调拨单: orderNo={}", orderNo);
        AllocationOrder o = new AllocationOrder();
        o.setOrderNo(orderNo); o.setSku(state.value("sku",""));
        o.setProductName(state.value("productName","")); o.setRequiredQty(state.value("qty",0));
        o.setStatus("PENDING"); orderMapper.insert(o);
        Map<String, Object> r = new LinkedHashMap<>(); r.put("orderId", o.getId());
        try { AllocationLog l = new AllocationLog(); l.setOrderNo(orderNo); l.setStepName("创建调拨单"); l.setStatus("OK"); l.setDetail("节点执行成功"); l.setStartAt(LocalDateTime.now()); l.setEndAt(LocalDateTime.now()); logMapper.insertLog(l); } catch (Exception e) { log.warn("Log: {}", e.getMessage()); }
        return r;
    }
}