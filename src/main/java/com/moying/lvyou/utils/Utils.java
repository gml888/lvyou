package com.moying.lvyou.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 工具类
 */
@SuppressWarnings("ALL")
public class Utils {
  /**
   * md5 加密
   *
   * @param pwd 密码字符串
   * @return String
   */
  public static String md5(String pwd) {
    try {
      // 创建加密对象
      MessageDigest digest = MessageDigest.getInstance("md5");

      // 调用加密对象的方法，加密的动作已经完成
      byte[] bs = digest.digest(pwd.getBytes());
      // 接下来，我们要对加密后的结果，进行优化，按照mysql的优化思路走
      // mysql的优化思路：
      // 第一步，将数据全部转换成正数：
      StringBuilder hexString = new StringBuilder();
      for (byte b : bs) {
        // 第一步，将数据全部转换成正数：
        int temp = b & 255;
        // 第二步，将所有的数据转换成16进制的形式
        if (temp < 16) {
          // 手动补上一个“0”
          hexString.append("0").append(Integer.toHexString(temp));
        } else {
          hexString.append(Integer.toHexString(temp));
        }
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return "";
  }

  /**
   * 取得当前格式化时间
   *
   * @return String
   */
  public static String getNowDateTime() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date date = new Date();
    return sdf.format(date);
  }
  /**
   * 取得当前格式化时间
   *
   * @return String
   */
  public static String getNowDateLocalTime() {
    SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
    Date date1 = new Date();
    SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");
    Date date2 = new Date();
    return sdf1.format(date1)+"T"+sdf2.format(date2);
  }

  /**
   * 计算两点之间的距离, 单位：米
   *
   * @param lat 用户坐标点纬度
   * @param lng 用户坐标点经度
   * @param postionlat 景点坐标点纬度
   * @param postionlng 景点坐标点经度
   * @return Float
   */
  public static Float getDistance(Float lat, Float lng, Float postionlat, Float postionlng) {
    if (lat != null && lng != null && postionlat != null && postionlng != null) {
      double earthRadius = 6378137.0; // 地球半径，单位：米
      double radLat1 = Math.toRadians(lat);
      double radLat2 = Math.toRadians(postionlat);
      double radLng1 = Math.toRadians(lng);
      double radLng2 = Math.toRadians(postionlng);

      double a = radLat2 - radLat1;
      double b = radLng2 - radLng1;

      double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
        Math.cos(radLat1) * Math.cos(radLat2) *
          Math.pow(Math.sin(b / 2), 2)));
      s = s * earthRadius; // 将弧度距离转换为米
      return (float) Math.round(Math.abs(s) * 10000) / 10000; // 四舍五入到小数点后四位，并确保为正数
    }
    return 0f;
  }

  /**
   * 计算两个时间之差(单位:秒)
   *
   * @param time1 第一个时间
   * @param time2 第二个时间
   * @return Integer
   */
  public static Integer getTime(String time1, String time2) {
    // 定义时间格式
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    try {
      // 解析时间字符串
      LocalDateTime dateTime1 = LocalDateTime.parse(time1, formatter);
      LocalDateTime dateTime2 = LocalDateTime.parse(time2, formatter);

      // 计算时间差（以秒为单位）
      long seconds = ChronoUnit.SECONDS.between(dateTime1, dateTime2);

      // 返回时间差的绝对值
      return Math.abs((int) seconds);
    } catch (Exception e) {
      // 处理解析异常
      e.printStackTrace();
      return 0;
    }
  }

  /**
   * 根据18位身份证号码判断性别
   *
   * @param idCard 18位的身份证号码
   * @return 性别，0(男)或1(女)
   * @throws IllegalArgumentException 如果身份证号码格式不正确
   * <p>
   * 身份证号码规则：
   * - 第17位表示性别，奇数为男性，偶数为女性。
   * - 第18位为校验码，可能是数字或字母X/x。
   */
  public static Integer getGenderFromIdCard(String idCard) {
    if (idCard == null || idCard.length() != 18 || !idCard.matches("\\d{17}[\\dXx]")) {
      throw new IllegalArgumentException("身份证号码不正确");
    }

    // 获取第17位数字（索引为16）
    char genderCode = idCard.charAt(16);

    // 判断性别，奇数为男，偶数为女
    if (Character.getNumericValue(genderCode) % 2 == 0) {
      return 1; // 女性
    } else {
      return 0; // 男性
    }
  }
}
