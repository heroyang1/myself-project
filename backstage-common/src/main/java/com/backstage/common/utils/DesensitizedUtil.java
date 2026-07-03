package com.backstage.common.utils;

/**
 * 脱敏工具类
 *
 * @author ruoyi
 */
public class DesensitizedUtil
{
    /**
     * 密码的全部字符都用*代替，比如：******
     *
     * @param password 密码
     * @return 脱敏后的密码
     */
    public static String password(String password)
    {
        if (StringUtils.isBlank(password))
        {
            return StringUtils.EMPTY;
        }
        return StringUtils.repeat('*', password.length());
    }

    /**
     * 车牌中间用*代替，如果是错误的车牌，不处理
     *
     * @param carLicense 完整的车牌号
     * @return 脱敏后的车牌
     */
    public static String carLicense(String carLicense)
    {
        if (StringUtils.isBlank(carLicense))
        {
            return StringUtils.EMPTY;
        }
        // 普通车牌
        if (carLicense.length() == 7)
        {
            carLicense = StringUtils.hide(carLicense, 3, 6);
        }
        else if (carLicense.length() == 8)
        {
            // 新能源车牌
            carLicense = StringUtils.hide(carLicense, 3, 7);
        }
        return carLicense;
    }

    /**
     * 订单号脱敏：保留前4位和后4位，中间用*代替
     * 例: GP20260624123456 → GP20****3456
     *
     * @param orderNo 订单号
     * @return 脱敏后的订单号
     */
    public static String maskOrderNo(String orderNo)
    {
        if (StringUtils.isBlank(orderNo) || orderNo.length() <= 8)
        {
            return orderNo;
        }
        return orderNo.substring(0, 4) + StringUtils.repeat('*', orderNo.length() - 8) + orderNo.substring(orderNo.length() - 4);
    }

    /**
     * IP地址脱敏：保留前两段，后两段用*代替
     * 例: 192.168.1.100 → 192.168.*.*
     *
     * @param ip IP地址
     * @return 脱敏后的IP
     */
    public static String maskIp(String ip)
    {
        if (StringUtils.isBlank(ip))
        {
            return StringUtils.EMPTY;
        }
        String[] parts = ip.split("\\.");
        if (parts.length == 4)
        {
            return parts[0] + "." + parts[1] + ".*.*";
        }
        return ip;
    }

    /**
     * 手机号脱敏：保留前3位和后4位，中间用****代替
     * 例: 13812345678 → 138****5678
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone)
    {
        if (StringUtils.isBlank(phone) || phone.length() < 7)
        {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 通用字符串脱敏：保留前N位和后N位，中间用*代替
     *
     * @param str     原始字符串
     * @param prefix  保留前缀长度
     * @param suffix  保留后缀长度
     * @return 脱敏后的字符串
     */
    public static String mask(String str, int prefix, int suffix)
    {
        if (StringUtils.isBlank(str))
        {
            return StringUtils.EMPTY;
        }
        int len = str.length();
        if (len <= prefix + suffix)
        {
            return StringUtils.repeat('*', len);
        }
        return str.substring(0, prefix) + StringUtils.repeat('*', len - prefix - suffix) + str.substring(len - suffix);
    }
}
