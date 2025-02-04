package com.huanzhen.fileflexmanager.infrastructure.util;

/**
 * 字节转换工具类
 */
public class ByteUtils {
    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB"};

    /**
     * 将字节数转换为人类可读的格式
     * @param bytes 字节数
     * @return 格式化后的字符串，如 "1.23 MB"
     */
    public static String humanReadableByteCount(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        exp = Math.min(exp, 4); // 限制到TB
        double value = bytes / Math.pow(1024, exp);
        return String.format("%.2f %s", value, UNITS[exp]);
    }

    /**
     * 将可能包含逗号的数字字符串转换为long
     * 例如: "1,234,567" -> 1234567
     * @param numberStr 包含逗号的数字字符串
     */
    public static String humanReadableByteCount(String numberStr) {
        if (numberStr == null || numberStr.trim().isEmpty()) {
            return null;
        }
        return humanReadableByteCount(Long.parseLong(numberStr.replace(",", "")));
    }

    /**
     * 解析人类可读的字节数字符串为字节数
     * 例如: "1.23MB" -> 1289748
     * @param humanReadable 人类可读的字节数字符串
     * @return 字节数
     */
    public static long parseHumanReadableByteCount(String humanReadable) {
        if (humanReadable == null || humanReadable.trim().isEmpty()) {
            return 0;
        }

        String trimmed = humanReadable.trim().toUpperCase();
        // 匹配数字部分和单位部分
        String[] parts = trimmed.split("(?<=\\d)(?=[A-Z])");
        if (parts.length != 2) {
            // 如果没有单位，假设是字节数
            return Long.parseLong(trimmed.replace(",", ""));
        }

        double number = Double.parseDouble(parts[0].replace(",", ""));
        String unit = parts[1];

        int unitIndex = -1;
        for (int i = 0; i < UNITS.length; i++) {
            if (unit.startsWith(UNITS[i])) {
                unitIndex = i;
                break;
            }
        }

        if (unitIndex == -1) {
            throw new IllegalArgumentException("Unknown unit: " + unit);
        }

        return (long) (number * Math.pow(1024, unitIndex));
    }
} 