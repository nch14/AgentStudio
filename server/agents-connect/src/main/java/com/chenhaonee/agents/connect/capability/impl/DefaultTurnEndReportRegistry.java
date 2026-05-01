package com.chenhaonee.agents.connect.capability.impl;

import com.chenhaonee.agents.connect.capability.TurnEndReport;
import com.chenhaonee.agents.connect.capability.TurnEndReportRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的 TurnEndReportRegistry 实现。
 * 单实例部署下安全；多副本场景需迁至 Redis/DB。
 */
@Component
public class DefaultTurnEndReportRegistry implements TurnEndReportRegistry {

    private final ConcurrentHashMap<String, TurnEndReport> store = new ConcurrentHashMap<>();

    @Override
    public void report(String turnCode, TurnEndReport report) {
        store.put(turnCode, report);
    }

    @Override
    public TurnEndReport consume(String turnCode) {
        return store.remove(turnCode);
    }
}
