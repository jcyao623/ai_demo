package com.aidemo.service.controller;

import com.aidemo.common.dto.Result;
import com.aidemo.service.service.WmsAllocationService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/wms")
@Slf4j
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
        return Result.success(r);
    }

    @GetMapping("/test")
    public Result<?> test() {
        String orderNo = "TST-" + UUID.randomUUID().toString().substring(0,6).toUpperCase();
        log.info("test: orderNo={} sku={} qty={}", orderNo, "SKU-001", 100);
        Map<String,Object> r = svc.allocate(orderNo, "SKU-001", "A1", 100);
        return Result.success(r);
    }

    @GetMapping("/order/{o}")
    public Result<Map> getOrder(@PathVariable String o) { return Result.success(svc.getOrder(o)); }

    @GetMapping("/review/list")
    public Result<?> pendingReviews() {
        return Result.success(svc.listPendingReviews());
    }

    @PostMapping("/review/approve")
    public Result<?> approve(@RequestParam String orderNo, @RequestParam(defaultValue = "") String comment) {
        return Result.success(svc.approveReview(orderNo, comment));
    }

    @PostMapping("/review/reject")
    public Result<?> reject(@RequestParam String orderNo, @RequestParam(defaultValue = "") String comment) {
        return Result.success(svc.rejectReview(orderNo, comment));
    }
}