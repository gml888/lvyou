package com.moying.lvyou.Entity;

import java.util.Date;

import lombok.Data;

import javax.persistence.*;

/**
 * 报名信息表
 */
@Data
@Entity(name = "Enlist")
@Table(name = "enlist")
public class Enlist {
  /**
   * 自增ID
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "eid", nullable = false)
  private Integer eid;

  /**
   * 用户id
   */
  @Column(name = "userid", nullable = false)
  private Integer userid;

  /**
   * 活动id
   */
  @Column(name = "infoid", nullable = false)
  private Integer infoid;

  /**
   * 是否显示
   */
  @Column(name = "isshow", nullable = false)
  private Byte isshow;

  /**
   * 报名时间
   */
  @Column(name = "createtime", nullable = false)
  private Date createtime;

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
