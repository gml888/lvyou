package com.moying.lvyou.Entity;

import java.util.Date;

import lombok.Data;

import javax.persistence.*;

/**
 * 操作员信息表
 */

@Data
@Entity(name = "Manage")
@Table(name = "manage")
public class Manage {
  /**
   * 自增列
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;

  /**
   * 姓名
   */
  @Column(name = "name", nullable = false, length = 50)
  private String name;

  /**
   * 用户名
   */
  @Column(name = "username", nullable = false, length = 50)
  private String username;

  /**
   * 密码
   */
  @Column(name = "password", nullable = false, length = 60)
  private String password;

  /**
   * 角色
   */
  @Column(name = "roleId", nullable = false)
  private Integer roleId;

  /**
   * 电子邮件
   */
  @Column(name = "email", nullable = false, length = 100)
  private String email;

  /**
   * 状态
   */
  @Column(name = "status", nullable = false)
  private Byte status;

  /**
   * 添加时间
   */
  @Column(name = "createTime", nullable = false)
  private Date createTime;
}
