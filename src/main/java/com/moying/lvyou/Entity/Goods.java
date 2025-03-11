package com.moying.lvyou.Entity;

import java.util.Date;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity(name = "Goods")
@Table(name = "goods")
public class Goods {
  /**
   * 自增ID
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "gid", nullable = false)
  private Integer gid;

  /**
   * 名称
   */
  @Column(name = "name", nullable = false, length = 100)
  private String name;

  /**
   * 图片
   */
  @Column(name = "pic", nullable = false, length = 200)
  private String pic;

  /**
   * 每人在一个活动中最多可领取数量
   */
  @Column(name = "count", nullable = false)
  private Integer count;

  /**
   * 介绍
   */
  @Column(name = "content", nullable = false, length = 500)
  private String content;

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

  /**
   * 商家ID
   */
  @Column(name = "shopid", nullable = false)
  private Integer shopid;

}
