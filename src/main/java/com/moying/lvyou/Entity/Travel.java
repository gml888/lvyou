package com.moying.lvyou.Entity;

import java.util.Date;

import lombok.Data;

import javax.persistence.*;

/**
 * 徒步旅行任务(参与排名:有效且已完成)
 */

@Data
@Entity(name = "Travel")
@Table(name = "travel")
public class Travel {
  /**
   * 自增id
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "tid", nullable = false)
  private Integer tid;

  /**
   * 用户id
   */
  @Column(name = "uid", nullable = false)
  private Integer uid;

  /**
   * 线路id
   */
  @Column(name = "rid", nullable = false)
  private Integer rid;

  /**
   * 总计路程
   */
  @Column(name = "distance", nullable = false)
  private Integer distance;

  /**
   * 所用时间
   */
  @Column(name = "time", nullable = false)
  private Integer time;

  /**
   * 创建时间
   */
  @Column(name = "createtime", nullable = false)
  private Date createtime;

  /**
   * 身份证号码
   */
  @Column(name = "cardno", nullable = false, length = 50)
  private String cardno;

  /**
   * 手机号
   */
  @Column(name = "tel", nullable = false, length = 20)
  private String tel;

  /**
   * 真实姓名
   */
  @Column(name = "name", nullable = false, length = 50)
  private String name;

  /**
   * 速度
   */
  @Column(name = "speed", nullable = false)
  private Float speed;

  /**
   * 获得积分
   */
  @Column(name = "points", nullable = false)
  private Integer points;

  /**
   * 是否激活本次旅行活动(报名审核是否通过)
   */
  @Column(name = "isactive", nullable = false)
  private Byte isactive;

  /**
   * 是否激活打卡环节
   */
  @Column(name = "isticket", nullable = false)
  private Byte isticket;

  /**
   * 本次活动是否结束
   */
  @Column(name = "isfinish", nullable = false)
  private Byte isfinish;

  /**
   * 是否有效
   */
  @Column(name = "isshow", nullable = false)
  private Byte isshow;

  /**
   * 性别 0:男 1:女
   */
  @Column(name = "sex", nullable = false)
  private Byte sex;

}
