package com.moying.lvyou.Entity;

import java.util.Date;

import lombok.Data;

import javax.persistence.*;

/**
 * 打卡表
 */

@Data
@Entity(name = "TickOff")
@Table(name = "tick_off")
public class TickOff {
  /**
   * 自增ID
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "tickid", nullable = false)
  private Integer tickid;

  /**
   * 用户id
   */
  @Column(name = "uid", nullable = false)
  private Integer uid;

  /**
   * 旅行id
   */
  @Column(name = "tid", nullable = false)
  private Integer tid;

  /**
   * 线路id
   */
  @Column(name = "rid", nullable = false)
  private Integer rid;

  /**
   * 景点id
   */
  @Column(name = "pid", nullable = false)
  private Integer pid;

  /**
   * 获得积分
   */
  @Column(name = "points", nullable = false)
  private Integer points;

  /**
   * 路程
   */
  @Column(name = "distance", nullable = false)
  private Integer distance;

  /**
   * 所用时间
   */
  @Column(name = "time", nullable = false)
  private Integer time;

  /**
   * 打卡时间
   */
  @Column(name = "createtime", nullable = false)
  private Date createtime;

  /**
   * 维度
   */
  @Column(name = "lat", nullable = false)
  private String lat;

  /**
   * 经度
   */
  @Column(name = "lng", nullable = false)
  private Date lng;
}
