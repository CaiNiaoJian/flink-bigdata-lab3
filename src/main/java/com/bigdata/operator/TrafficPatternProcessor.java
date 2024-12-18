package com.bigdata.operator;

import com.bigdata.model.TollEvent;
import com.bigdata.utils.TimeUtils;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.streaming.api.windowing.time.Time;

public class TrafficPatternProcessor {
    private static final int VEHICLE_COUNT_THRESHOLD = 100; // 车流量阈值
    private static final int PATTERN_WINDOW_MINUTES = 5;

    public static Pattern<TollEvent, TollEvent> createTrafficPattern() {
        return Pattern.<TollEvent>begin("first")
                .where(new SimpleCondition<TollEvent>() {
                    @Override
                    public boolean filter(TollEvent event) {
                        // 检测车流量激增
                        return true; // 具体逻辑在处理函数中实现
                    }
                })
                .next("second")
                .where(new SimpleCondition<TollEvent>() {
                    @Override
                    public boolean filter(TollEvent event) {
                        return true; // 具体逻辑在处理函数中实现
                    }
                })
                .within(Time.minutes(PATTERN_WINDOW_MINUTES));
    }

    public static String createAlertMessage(TollEvent event) {
        return String.format("交通预警: 卡口 %s（%s）在 %s 检测到车流量异常，方向：%s",
                event.getKkmc(),
                event.getXzqhmc(),
                TimeUtils.formatDateTime(event.getGcsj()),
                event.getFxlx());
    }

    /**
     * 判断是否需要触发预警
     * @param currentCount 当前车流量
     * @param averageCount 平均车流量
     * @return 是否触发预警
     */
    public static boolean shouldTriggerAlert(long currentCount, long averageCount) {
        // 当前车流量超过平均值的50%时触发预警
        return currentCount > averageCount * 1.5;
    }
} 