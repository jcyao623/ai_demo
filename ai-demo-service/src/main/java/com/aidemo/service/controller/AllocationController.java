package com.aidemo.service.controller;

import com.aidemo.common.dto.Result;
import com.aidemo.service.service.WmsAllocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/wms")
public class AllocationController {
    private static final Logger log = LoggerFactory.getLogger(AllocationController.class);
    private final WmsAllocationService svc;
    public AllocationController(WmsAllocationService s) { this.svc = s; }
    @GetMapping("/graph")
    public Result<Map> getGraph() { return Result.success(svc.structure()); }
    @PostMapping("/allocate")
    public Result<?> allocate(@RequestParam(defaultValue = "SKU-001") String sku, @RequestParam(defaultValue = "A1") String pn, @RequestParam(defaultValue = "100") int qty) {
        String o = "WO-" + UUID.randomUUID().toString().substring(0,8).toUpperCase();
        log.info("alloc: o={} sku={} qty={}", o, sku, qty);
        Map<String,Object> r = svc.allocate(o, sku, pn, qty);
        boolean ok = "SUCCESS".equals(r.get("overall"));
        if (ok) return Result.success(r); r.put("_error", true); return Result.success(r);
    }
    @GetMapping("/order/{o}")
    public Result<Map> getOrder(@PathVariable String o) { return Result.success(svc.getOrder(o)); }
    @GetMapping("/test")
    public Result<?> test() {
        String o = "TST-" + UUID.randomUUID().toString().substring(0,6).toUpperCase();
        Map<String,Object> r = svc.allocate(o, "SKU-001", "A1", 100);
        boolean ok = "SUCCESS".equals(r.get("overall"));
        if (ok) return Result.success(r); r.put("_error", true); return Result.success(r);
    }
}