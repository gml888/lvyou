package com.moying.lvyou.Entity;

import java.util.Date;

import lombok.Data;

import javax.persistence.*;

/*
 * 勋章模板信息表
 */
@Data
@Entity(name = "MedalTemplate")
@Table(name = "medal_template")
public class MedalTemplate {
  /**
   * 自增ID
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "mid", nullable = false)
  private Integer mid;

  /**
   * 线路id
   */
  @Column(name = "roadid", nullable = false)
  private Integer roadid;

  /**
   * 名称
   */
  @Column(name = "name", nullable = false, length = 50)
  private String name;

  /**
   * 图片
   */
  @Column(name = "pic", nullable = false, length = 200)
  private String pic;

  /**
   * 是否显示
   */
  @Column(name = "isshow", nullable = false)
  private Byte isshow;

  /**
   * 发布时间
   */
  @Column(name = "createtime", nullable = false)
  private Date createtime;
}
