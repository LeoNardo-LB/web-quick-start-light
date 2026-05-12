package org.smm.archetype.shared.logging;

import cn.hutool.core.util.StrUtil;

public final class SensitiveLogUtils {

    private static final double DEFAULT_RATIO = 0.75;

    private SensitiveLogUtils() {}

    public static String mask(String value) {
        return mask(value, DEFAULT_RATIO);
    }

    public static String mask(String value, double ratio) {
        if (StrUtil.isBlank(value))
            return value;
        int len = value.length();
        if (len <= 2)
            return value.charAt(0) + "*";
        int maskLen = Math.max(1, (int) Math.round(len * ratio));
        int maxMaskLen = len - 2;
        maskLen = Math.min(maskLen, maxMaskLen);
        int totalKeep = len - maskLen;
        int keepStart = totalKeep / 2;
        int keepEnd = len - (totalKeep - keepStart);
        StringBuilder sb = new StringBuilder();
        sb.append(value, 0, keepStart);
        sb.repeat("*", maskLen);
        sb.append(value.substring(keepEnd));
        return sb.toString();
    }

}
