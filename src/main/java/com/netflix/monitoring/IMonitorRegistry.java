package com.netflix.monitoring;

public interface IMonitorRegistry {
    void registerObject(Object obj);
    void unRegisterObject(Object obj);
}
