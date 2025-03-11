package com.moying.lvyou.Entity;

import java.util.Date;

import lombok.Data;

import javax.persistence.*;

/**
 * 线路信息表
 */

@Data
@Entity(name = "Road")
@Table(name = "road")
public class Road {
  /**
   * 自增ID
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "rid", nullable = false)
  private Integer rid;

  /**
   * 名称
   */
  @Column(name = "name", nullable = false, length = 50)
  private String name;

  /**
   * 线路图片
   */
  @Column(name = "pic", nullable = false, length = 200)
  private String pic;

  /**
   * 线路简图
   */
  @Column(name = "roadpic", nullable = false, length = 200)
  private String roadpic;

  /**
   * 线路介绍
   */
  @Column(name = "content", nullable = false, length = 500)
  private String content;

  /**
   * 创建时间
   */
  @Column(name = "createtime", nullable = false)
  private Date createtime;

  /**
   * 显示状态
   */
  @Column(name = "isshow", nullable = false)
  private Byte isshow;

  /**
   * 商品ID字符串
   */
  @Column(name = "goodsidstr", nullable = false, length = 500)
  private String goodsidstr;

}
