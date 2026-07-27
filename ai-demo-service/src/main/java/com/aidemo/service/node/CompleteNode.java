package com.aidemo.service.node;

import com.aidemo.service.entity.AllocationLog;
import com.aidemo.service.mapper.AllocationLogMapper;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component("completeNode")
public class CompleteNode implements NodeAction {
    private static final Logger log = LoggerFactory.getLogger(CompleteNode.class);
    private final AllocationLogMapper logMapper;
    public CompleteNode(AllocationLogMapper l) { this.logMapper = l; }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("[Node] 完成: orderNo={}", state.value("orderNo",""));
        try { AllocationLog l = new AllocationLog(); l.setOrderNo(state.value("orderNo","")); l.setStepName("完成"); l.setStatus("OK"); l.setDetail("调拨流程执行完成"); l.setStartAt(LocalDateTime.now()); l.setEndAt(LocalDateTime.now()); logMapper.insertLog(l); } catch (Exception e) { log.warn("Log: {}", e.getMessage()); }
        return new LinkedHashMap<>();
    }
}