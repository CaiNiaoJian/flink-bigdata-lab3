package com.bigdata.operator;

import com.bigdata.model.TollEvent;
import com.bigdata.model.TollStats;
import com.bigdata.utils.TimeUtils;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.util.HashMap;
import java.util.Map;

public class TollStatsWindowFunction extends ProcessWindowFunction<TollEvent, TollStats, String, TimeWindow> {
    @Override
    public void process(String kkmc,
                       Context context,
                       Iterable<TollEvent> events,
                       Collector<TollStats> out) {
        long count = 0;
        String xzqhmc = null;
        Map<String, Integer> fxlxCount = new HashMap<>();
        
        // 统计各个方向的车流量
        for (TollEvent event : events) {
            count++;
            xzqhmc = event.getXzqhmc(); // 获取行政区划名称
            fxlxCount.merge(event.getFxlx(), 1, Integer::sum);
        }
        
        // 找出车流量最大的方向
        String mainFxlx = fxlxCount.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("未知");
        
        // 使用TimeUtils格式化窗口时间
        String windowStart = TimeUtils.formatWindowTime(context.window().getStart());
        String windowEnd = TimeUtils.formatWindowTime(context.window().getEnd());
        
        out.collect(new TollStats(
                kkmc,
                xzqhmc,
                windowStart,
                windowEnd,
                count,
                mainFxlx
        ));
    }
} 