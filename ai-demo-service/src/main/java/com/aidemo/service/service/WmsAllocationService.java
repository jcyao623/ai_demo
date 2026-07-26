package com.aidemo.service.service;

import com.aidemo.service.mapper.AllocationOrderMapper;
import javax.sql.DataSource;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class WmsAllocationService {
    private static final Logger log = LoggerFactory.getLogger(WmsAllocationService.class);
    private final StateMachineFactory<String,String> factory;
    private final AllocationOrderMapper orderMapper;
    private DataSource dataSource;

    public WmsAllocationService(StateMachineFactory<String,String> f, AllocationOrderMapper o) { factory=f; orderMapper=o; }

    public Map<String,Object> allocate(String orderNo, String sku, String pn, int qty) {
        StateMachine<String,String> sm = factory.getStateMachine("wms-"+orderNo);
        sm.start();
        Map<String,Object> v = (Map)sm.getExtendedState().getVariables();
        v.put("orderNo",orderNo); v.put("sku",sku); v.put("productName",pn); v.put("qty",qty);
        if (!step(sm,"NEXT","C->I")) return result(sm,false);
        if (!step(sm,"NEXT","I->A")) return result(sm,false);
        if (!step(sm,"NEXT","A->E")) return result(sm,false);
        if (!step(sm,"NEXT","E->C")) return result(sm,false);
        return result(sm,true);
    }
    private boolean step(StateMachine<String,String> sm, String ev, String d) {
        log.info("[SM] {} {}",ev,d); boolean s=sm.sendEvent(ev);
        return s && !"FAILED".equals(sm.getState().getId());
    }
    private Map<String,Object> result(StateMachine<String,String> sm, boolean ok) {
        Map<String,Object> m=new LinkedHashMap<>(); m.put("overall",ok?"SUCCESS":"FAILED");
        m.putAll((Map)sm.getExtendedState().getVariables()); m.put("finalState",sm.getState().getId()); return m;
    }
    public Map<String,Object> getOrder(String orderNo) {
        var o = orderMapper.selectOne(new LambdaQueryWrapper<com.aidemo.service.entity.AllocationOrder>().eq(com.aidemo.service.entity.AllocationOrder::getOrderNo, orderNo));
        if (o==null) return Map.of("error","not found");
        Map<String,Object> m=new LinkedHashMap<>(); m.put("orderNo",o.getOrderNo()); m.put("sku",o.getSku());
        m.put("requiredQty",o.getRequiredQty()); m.put("allocatedQty",o.getAllocatedQty()); m.put("status",o.getStatus()); return m;
    }
    public Map<String,Object> structure() {
        Map<String,Object> s=new LinkedHashMap<>(); s.put("framework","Spring State Machine");
        s.put("states",List.of("CREATE_ORDER","CHECK_INVENTORY","AI_DECIDE","EXECUTE","COMPLETE")); return s;
    }
}
