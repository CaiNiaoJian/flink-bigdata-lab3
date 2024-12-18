package com.bigdata.model;

public class TollStats {
    private String kkmc;          // 卡口名称
    private String xzqhmc;        // 行政区划名称
    private String windowStart;    // 窗口开始时间
    private String windowEnd;      // 窗口结束时间
    private long vehicleCount;     // 车辆数量
    private String fxlx;          // 主要方向类型

    public TollStats() {}

    public TollStats(String kkmc, String xzqhmc, String windowStart, String windowEnd, 
                    long vehicleCount, String fxlx) {
        this.kkmc = kkmc;
        this.xzqhmc = xzqhmc;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.vehicleCount = vehicleCount;
        this.fxlx = fxlx;
    }

    // Getters and Setters
    public String getKkmc() { return kkmc; }
    public void setKkmc(String kkmc) { this.kkmc = kkmc; }

    public String getXzqhmc() { return xzqhmc; }
    public void setXzqhmc(String xzqhmc) { this.xzqhmc = xzqhmc; }

    public String getWindowStart() { return windowStart; }
    public void setWindowStart(String windowStart) { this.windowStart = windowStart; }

    public String getWindowEnd() { return windowEnd; }
    public void setWindowEnd(String windowEnd) { this.windowEnd = windowEnd; }

    public long getVehicleCount() { return vehicleCount; }
    public void setVehicleCount(long vehicleCount) { this.vehicleCount = vehicleCount; }

    public String getFxlx() { return fxlx; }
    public void setFxlx(String fxlx) { this.fxlx = fxlx; }

    @Override
    public String toString() {
        return "TollStats{" +
                "kkmc='" + kkmc + '\'' +
                ", xzqhmc='" + xzqhmc + '\'' +
                ", windowStart='" + windowStart + '\'' +
                ", windowEnd='" + windowEnd + '\'' +
                ", vehicleCount=" + vehicleCount +
                ", fxlx='" + fxlx + '\'' +
                '}';
    }
} 