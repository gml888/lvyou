package com.moying.lvyou.Entity;

import lombok.Data;

import javax.persistence.*;

/**
 * 报名用户信息表
 */
@Data
@Entity(name = "EnlistUserid")
@Table(name = "enlist_userid")
public class EnlistUserid {
  /**
   * 自增ID
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "euid", nullable = false)
  private Integer euid;

  /**
   * 报名id
   */
  @Column(name = "eid", nullable = false)
  private Integer eid;

  /**
   * 用户id
   */
  @Column(name = "userid", nullable = false)
  private Integer userid;

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
   * 备注
   */
  @Column(name = "memo", nullable = false, length = 500)
  private String memo;
}
