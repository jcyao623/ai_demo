package com.aidemo.service.service;

import com.aidemo.service.entity.AllocationOrder;
import com.aidemo.service.mapper.AllocationOrderMapper;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.OverAllState.HumanFeedback;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class WmsAllocationService {
    private static final Logger log = LoggerFactory.getLogger(WmsAllocationService.class);
    private final CompiledGraph wmsGraph;
    private final AllocationOrderMapper orderMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public WmsAllocationService(CompiledGraph g, AllocationOrderMapper o, RedisTemplate<String, Object> rt) { this.wmsGraph = g; this.orderMapper = o; this.redisTemplate = rt; }

    public Map<String,Object> allocate(String orderNo, String sku, String pn, int qty) {
        log.info("[WMS] 启动调拨: orderNo={}, sku={}, qty={}", orderNo, sku, qty);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", orderNo);
        try {
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("orderNo", orderNo); input.put("sku", sku);
            input.put("productName", pn); input.put("qty", qty);
            RunnableConfig config = RunnableConfig.builder().threadId(orderNo).build();
            wmsGraph.invoke(input, config);
            result.put("overall", "PENDING_REVIEW");
            result.put("message", "AI决策完成，等待人工审核");
            log.info("[WMS]  调拨等待审核: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("[WMS] 调拨失败: {}", e.getMessage());
            result.put("overall", "FAILED"); result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String,Object> approveReview(String orderNo, String comment) {
        return resumeReview(orderNo, "approved", comment);
    }

    public Map<String,Object> rejectReview(String orderNo, String comment) {
        return resumeReview(orderNo, "rejected", comment);
    }

    private Map<String,Object> resumeReview(String orderNo, String result, String comment) {
        log.info("[WMS] {}审核: orderNo={}", "approved".equals(result) ? "通过" : "拒绝", orderNo);
        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("orderNo", orderNo);
        try {
            RunnableConfig config = RunnableConfig.builder().threadId(orderNo).build();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("result", result); data.put("comment", comment != null ? comment : "");
            String nextNode = "approved".equals(result) ? "execute" : "complete";
            HumanFeedback feedback = new HumanFeedback(data, nextNode);
            wmsGraph.resume(feedback, config);
            resultMap.put("overall", "SUCCESS");
            resultMap.put("reviewResult", result);
            redisTemplate.delete("review:" + orderNo);
            log.info("[WMS] 审核完成: orderNo={}, result={}", orderNo, result);
        } catch (Exception e) {
            log.error("[WMS] 审核失败: {}", e.getMessage());
            resultMap.put("overall", "FAILED"); resultMap.put("error", e.getMessage());
        }
        return resultMap;
    }

    public List<Map<String,Object>> listPendingReviews() {
        List<Map<String,Object>> list = new ArrayList<>();
        Set<String> keys = redisTemplate.keys("review:*");
        if (keys == null) return list;
        for (String key : keys) {
            @SuppressWarnings("unchecked")
            Map<String, Object> info = (Map<String, Object>) redisTemplate.opsForValue().get(key);
            if (info != null && "PENDING_REVIEW".equals(info.get("status"))) {
                list.add(info);
            }
        }
        return list;
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
        s.put("states",List.of("create-order","check-inventory","ai-decide","human-review","execute","complete"));
        s.put("edges",List.of(
            Map.of("from","create-order","to","check-inventory"),
            Map.of("from","check-inventory","to","ai-decide"),
            Map.of("from","ai-decide","to","human-review"),
            Map.of("from","human-review","to","execute"),
            Map.of("from","execute","to","complete")
        ));
        return s;
    }
}