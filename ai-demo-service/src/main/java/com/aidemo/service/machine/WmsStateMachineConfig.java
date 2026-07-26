package com.aidemo.service.machine;

import com.aidemo.service.entity.*;
import com.aidemo.service.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import java.time.LocalDateTime;
import java.util.*;

@Configuration
@EnableStateMachineFactory
public class WmsStateMachineConfig extends StateMachineConfigurerAdapter<String, String> {
    private static final Logger log = LoggerFactory.getLogger(WmsStateMachineConfig.class);
    private final AllocationOrderMapper orderMapper;
    private final InventoryMapper inventoryMapper;
    private final AllocationLogMapper logMapper;
    private final ChatClient chatClient;

    public WmsStateMachineConfig(AllocationOrderMapper o,
                                 InventoryMapper i,
                                 AllocationLogMapper l,
                                 ChatClient.Builder cb) {
        this.orderMapper = o;
        this.inventoryMapper = i;
        this.logMapper = l;
        this.chatClient = cb.build();
    }

    @Override
    public void configure(StateMachineStateConfigurer<String, String> states) throws Exception {
        states.withStates().initial("CREATE_ORDER")
            .state("CHECK_INVENTORY").state("AI_DECIDE")
            .state("EXECUTE").state("COMPLETE").state("FAILED")
            .end("COMPLETE").end("FAILED");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void configure(StateMachineTransitionConfigurer<String, String> t) throws Exception {
        t
        .withExternal().source("CREATE_ORDER").target("CHECK_INVENTORY").event("NEXT")
        .action(ctx -> {
            Map<String,Object> v = (Map)ctx.getExtendedState().getVariables();
            log.info("[SM] Step1 createOrder: {}", v.get("orderNo"));
            AllocationOrder oo = new AllocationOrder();
            oo.setOrderNo((String)v.get("orderNo")); oo.setSku((String)v.get("sku"));
            oo.setProductName((String)v.get("productName")); oo.setRequiredQty((Integer)v.get("qty"));
            oo.setStatus("PENDING"); orderMapper.insert(oo);
            v.put("orderId", oo.getId());
            AllocationLog al = new AllocationLog(); al.setOrderNo((String)v.get("orderNo"));
            al.setStepName("创建调拨单"); al.setStatus("OK"); al.setStartAt(LocalDateTime.now()); al.setEndAt(LocalDateTime.now());
            logMapper.insertLog(al);
        })
        .and()
        .withExternal().source("CHECK_INVENTORY").target("AI_DECIDE").event("NEXT")
        .action(ctx -> {
            Map<String,Object> v = (Map)ctx.getExtendedState().getVariables();
            log.info("[SM] Step2 checkInventory: sku={}", v.get("sku"));
            List<Map<String,Object>> stock = inventoryMapper.selectStockBySku((String)v.get("sku"));
            int total = stock.stream().mapToInt(r -> ((Number)r.get("avail")).intValue()).sum();
            int req = (Integer)v.get("qty");
            if (total < req) { ctx.getStateMachine().sendEvent("FAIL"); return; }
            v.put("stockInfo", stock); v.put("totalAvail", total);
        })
        .and()
        .withExternal().source("AI_DECIDE").target("EXECUTE").event("NEXT")
        .action(ctx -> {
            Map<String,Object> v = (Map)ctx.getExtendedState().getVariables();
            log.info("[SM] Step3 aiDecide: sku={}", v.get("sku"));
            List<Map<String,Object>> stock = (List<Map<String,Object>>)v.get("stockInfo");
            StringBuilder p = new StringBuilder("分配" + v.get("qty") + "件, 仓库:\n");
            for (Map<String,Object> wh : stock) p.append("- ").append(wh.get("code")).append(": 可用").append(wh.get("avail")).append("\n");
            p.append("返回JSON格式分配方案");
            String resp = chatClient.prompt().user(p.toString()).call().content();
            v.put("aiResp", resp != null ? resp : "");
        })
        .and()
        .withExternal().source("EXECUTE").target("COMPLETE").event("NEXT")
        .action(ctx -> {
            Map<String,Object> v = (Map)ctx.getExtendedState().getVariables();
            log.info("[SM] Step4 execute: sku={}", v.get("sku"));
            int q = (Integer)v.get("qty");
            int alloc = 0;
            for (Map<String,Object> wh : (List<Map<String,Object>>)v.get("stockInfo")) {
                int a = Math.min(((Number)wh.get("avail")).intValue(), q - alloc);
                if (a <= 0) break;
                inventoryMapper.updateAllocatedQty((String)v.get("sku"), (String)wh.get("code"), a);
                alloc += a;
            }
            orderMapper.updateStatus((String)v.get("orderNo"), "COMPLETED", alloc);
            v.put("allocated", alloc);
        })
        .and()
        .withExternal().source("CREATE_ORDER").target("FAILED").event("FAIL")
        .and()
        .withExternal().source("CHECK_INVENTORY").target("FAILED").event("FAIL");
    }
}