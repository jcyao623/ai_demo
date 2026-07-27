package com.aidemo.service.node;

import com.aidemo.service.entity.AllocationLog;
import com.aidemo.service.mapper.AllocationLogMapper;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component("humanReviewNode")
public class HumanReviewNode implements NodeAction {
    private static final Logger log = LoggerFactory.getLogger(HumanReviewNode.class);
    private final RedisTemplate<String, Object> redisTemplate;
    private final AllocationLogMapper logMapper;
    public HumanReviewNode(RedisTemplate<String, Object> rt, AllocationLogMapper lm) { this.redisTemplate = rt; this.logMapper = lm; }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String orderNo = state.value("orderNo", "");
        if (state.isResume()) {
            String result = state.humanFeedback() != null ? (String) state.humanFeedback().data().get("result") : "approved";
            log.info("[Node] 审核恢复: orderNo={}, result={}", orderNo, result);
            return Map.of("reviewResult", result);
        }
        log.info("[Node] 人工审核: orderNo={}", orderNo);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("orderNo", orderNo); info.put("status", "PENDING_REVIEW");
        info.put("aiResponse", state.value("aiResp", ""));
        redisTemplate.opsForValue().set("review:" + orderNo, info, 24, TimeUnit.HOURS);
        state.withHumanFeedback(new OverAllState.HumanFeedback(Map.of("status", "pending"), "execute"));
        logEntry(orderNo, "人工审核", "PENDING");
        return Map.of("reviewStatus", "pending");
    }
    private void logEntry(String orderNo, String step, String status) {
        try { AllocationLog l = new AllocationLog(); l.setOrderNo(orderNo); l.setStepName(step); l.setStatus(status); l.setDetail("等待审核"); l.setStartAt(LocalDateTime.now()); l.setEndAt(LocalDateTime.now()); logMapper.insertLog(l); } catch (Exception e) { log.warn("Log: {}", e.getMessage()); }
    }
}