package com.moying.lvyou.Entity;

import java.util.Date;

import lombok.Data;

import javax.persistence.*;

/**
 * 用户信息表
 */

@Data
@Entity(name = "User")
@Table(name = "user")
public class User {
  /**
   * 自增ID
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "userid", nullable = false)
  private Integer userid;

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
   * 昵称
   */
  @Column(name = "nickname", nullable = false, length = 50)
  private String nickname;

  /**
   * 头像
   */
  @Column(name = "headface", nullable = false)
  private String headface;

  /**
   * 姓名
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
   * 微信号
   */

  @Column(name = "wxnumber", nullable = false, length = 50)
  private String wxnumber;

  /**
   * 是否公开
   */
  @Column(name = "ispublic", nullable = false)
  private Boolean ispublic = false;

  /**
   * 是否会员
   */
  @Column(name = "ismember", nullable = false)
  private Byte ismember;

}
