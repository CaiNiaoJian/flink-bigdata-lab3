package com.bigdata.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 时间处理工具类
 */
public class TimeUtils {
    // 日期时间格式化器
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // 时间戳格式化器
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 将字符串格式的时间转换为Instant
     * @param dateTimeStr 格式：yyyy-MM-dd HH:mm:ss
     * @return Instant对象
     */
    public static Instant parseDateTime(String dateTimeStr) {
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
        return dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    /**
     * 将Instant转换为格式化的日期时间字符串
     * @param instant Instant对象
     * @return 格式化的时间字符串 (yyyy-MM-dd HH:mm:ss)
     */
    public static String formatDateTime(Instant instant) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return DATE_TIME_FORMATTER.format(dateTime);
    }

    /**
     * 将时间戳转换为紧凑格式的字符串
     * @param timestamp 时间戳
     * @return 格式化的时间戳字符串 (yyyyMMddHHmmss)
     */
    public static String formatTimestamp(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp), 
                ZoneId.systemDefault()
        );
        return TIMESTAMP_FORMATTER.format(dateTime);
    }

    /**
     * 获取当前时间的格式化字符串
     * @return 当前时间的格式化字符串 (yyyy-MM-dd HH:mm:ss)
     */
    public static String getCurrentDateTime() {
        return formatDateTime(Instant.now());
    }

    /**
     * 计算两个时间戳之间的时间差（分钟）
     * @param timestamp1 第一个时间戳
     * @param timestamp2 第二个时间戳
     * @return 时间差（分钟）
     */
    public static long getMinutesBetween(long timestamp1, long timestamp2) {
        return Math.abs(timestamp1 - timestamp2) / (60 * 1000);
    }

    /**
     * 将窗口时间转换为可读格式
     * @param windowTime 窗口时间戳
     * @return 格式化的时间字符串
     */
    public static String formatWindowTime(long windowTime) {
        return formatDateTime(Instant.ofEpochMilli(windowTime));
    }

    /**
     * 检查时间是否在指定的时间范围内
     * @param time 要检查的时间
     * @param start 开始时间
     * @param end 结束时间
     * @return 是否在范围内
     */
    public static boolean isTimeInRange(Instant time, Instant start, Instant end) {
        return !time.isBefore(start) && !time.isAfter(end);
    }
} 