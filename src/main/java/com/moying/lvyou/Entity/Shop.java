package com.moying.lvyou.Entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * 用户信息表
 */

@Data
@Entity(name = "Shop")
@Table(name = "shop")
public class Shop {
  /**
   * 自增ID
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "shopid", nullable = false)
  private Integer shopid;

  /**
   * 用户名
   */
  @Column(name = "username", nullable = false, length = 50)
  private String username;

  /**
   * 密码
   */
  @Column(name = "pwd", nullable = false, length = 64)
  private String pwd;

  /**
   * 小程序openid
   */
  @Column(name = "openid", nullable = false, length = 64)
  private String openid;


  /**
   * 头像
   */
  @Column(name = "headface", nullable = false)
  private String headface;

  /**
   * 商家名称
   */
  @Column(name = "name", nullable = false, length = 50)
  private String name;

  /**
   * 电话
   */
  @Column(name = "tel", nullable = false, length = 50)
  private String tel;

  /**
   * 状态 0.未知1.正常2.失效
   */
  @Column(name = "status", nullable = false)
  private byte status;

  /**
   * 创建时间
   */
  @Column(name = "createtime", nullable = false)
  private Date createtime;

  /**
   * 商家名称
   */
  @Column(name = "shopname", nullable = false, length = 50)
  private String shopname;

}
