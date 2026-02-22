package com.scit.soragodong.util;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

public class SystemResourceUtil {
    // OS 정보를 읽어오기 위한 관리 빈(Bean)을 가져와 캐스팅합니다.
    private static final OperatingSystemMXBean osBean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    public static Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. CPU 사용량 계산 (0.0 ~ 1.0 사이의 값을 %로 변환)
        double cpuLoad = osBean.getCpuLoad() * 100;
        if (cpuLoad < 0) cpuLoad = 0; // 초기 구동 시 측정 시간이 부족하면 0으로 처리

        // 2. 메모리 사용량 계산 (Byte 단위를 MB로 변환하여 계산)
        long totalMemory = osBean.getTotalMemorySize();
        long freeMemory = osBean.getFreeMemorySize();
        long usedMemory = totalMemory - freeMemory;
        double memUsagePercent = ((double) usedMemory / totalMemory) * 100;

        // 3. 결과 데이터를 맵에 담기 (소수점 첫째자리까지 반올림)
        stats.put("cpuUsage", Math.round(cpuLoad * 10) / 10.0);
        stats.put("memoryUsage", Math.round(memUsagePercent * 10) / 10.0);
        stats.put("status", "Online"); // 서버가 살아있음을 알림

        return stats;
    }
}
