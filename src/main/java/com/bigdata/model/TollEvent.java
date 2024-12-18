package com.bigdata.model;

import java.time.Instant;

public class TollEvent {
    private String gcxh;      // 过车序号
    private String xzqhmc;    // 行政区划名称
    private String kkmc;      // 卡口名称
    private String fxlx;      // 方向类型
    private Instant gcsj;     // 过车时间
    private String hpzl;      // 号牌种类
    private String hp;        // 号牌
    private String clppxh;    // 车辆品牌型号

    public TollEvent() {}

    public TollEvent(String gcxh, String xzqhmc, String kkmc, String fxlx, 
                    Instant gcsj, String hpzl, String hp, String clppxh) {
        this.gcxh = gcxh;
        this.xzqhmc = xzqhmc;
        this.kkmc = kkmc;
        this.fxlx = fxlx;
        this.gcsj = gcsj;
        this.hpzl = hpzl;
        this.hp = hp;
        this.clppxh = clppxh;
    }

    // Getters and Setters
    public String getGcxh() { return gcxh; }
    public void setGcxh(String gcxh) { this.gcxh = gcxh; }
    
    public String getXzqhmc() { return xzqhmc; }
    public void setXzqhmc(String xzqhmc) { this.xzqhmc = xzqhmc; }
    
    public String getKkmc() { return kkmc; }
    public void setKkmc(String kkmc) { this.kkmc = kkmc; }
    
    public String getFxlx() { return fxlx; }
    public void setFxlx(String fxlx) { this.fxlx = fxlx; }
    
    public Instant getGcsj() { return gcsj; }
    public void setGcsj(Instant gcsj) { this.gcsj = gcsj; }
    
    public String getHpzl() { return hpzl; }
    public void setHpzl(String hpzl) { this.hpzl = hpzl; }
    
    public String getHp() { return hp; }
    public void setHp(String hp) { this.hp = hp; }
    
    public String getClppxh() { return clppxh; }
    public void setClppxh(String clppxh) { this.clppxh = clppxh; }

    @Override
    public String toString() {
        return "TollEvent{" +
                "gcxh='" + gcxh + '\'' +
                ", xzqhmc='" + xzqhmc + '\'' +
                ", kkmc='" + kkmc + '\'' +
                ", fxlx='" + fxlx + '\'' +
                ", gcsj=" + gcsj +
                ", hpzl='" + hpzl + '\'' +
                ", hp='" + hp + '\'' +
                ", clppxh='" + clppxh + '\'' +
                '}';
    }
} 