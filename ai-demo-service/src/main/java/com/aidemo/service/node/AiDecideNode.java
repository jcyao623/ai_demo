package com.aidemo.service.node;

import com.aidemo.service.entity.AllocationLog;
import com.aidemo.service.mapper.AllocationLogMapper;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component("aiDecideNode")
public class AiDecideNode implements NodeAction {
    private static final Logger log = LoggerFactory.getLogger(AiDecideNode.class);
    private final ChatClient chatClient; private final AllocationLogMapper logMapper;
    public AiDecideNode(ChatClient.Builder cb, AllocationLogMapper l) { this.chatClient = cb.build(); this.logMapper = l; }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) {
        log.info("[Node] AI决策: sku={}", state.value("sku",""));
        @SuppressWarnings("unchecked") List<Map<String,Object>> stock = (List<Map<String,Object>>)(List)state.value("stockInfo", new java.util.ArrayList<>());
        StringBuilder p = new StringBuilder("分配" + state.value("qty",0) + "件:\n");
        for (Map<String,Object> wh : stock) p.append("- ").append(wh.get("code")).append(": 可用").append(wh.get("avail")).append("\n");
        p.append("返回JSON分配方案");
        String resp = chatClient.prompt().user(p.toString()).call().content();
        Map<String, Object> r = new LinkedHashMap<>(); r.put("aiResp", resp != null ? resp : "");
        try { AllocationLog l = new AllocationLog(); l.setOrderNo(state.value("orderNo","")); l.setStepName("AI分配决策"); l.setStatus("OK"); l.setDetail("AI已返回"); l.setStartAt(LocalDateTime.now()); l.setEndAt(LocalDateTime.now()); logMapper.insertLog(l); } catch (Exception e) { log.warn("Log: {}", e.getMessage()); }
        return r;
    }
}