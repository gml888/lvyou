package com.moying.lvyou.Entity;

import java.util.Date;

import lombok.Data;

import javax.persistence.*;

/**
 * 物品领用信息表
 */
@Data
@Entity(name = "GoodsUse")
@Table(name = "goods_use")
public class GoodsUse {
  /**
   * 自增ID
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "uid", nullable = false)
  private Integer uid;

  /**
   * 物品id
   */
  @Column(name = "goodsid", nullable = false)
  private Integer goodsid;

  /**
   * 领用用户id
   */
  @Column(name = "userid", nullable = false)
  private Integer userid;

  /**
   * 操作用户id
   */
  @Column(name = "operid", nullable = false)
  private Integer operid;

  /**
   * 操作人
   */
  @Column(name = "opername", nullable = false, length = 20)
  private String opername;

  /**
   * 领用数量
   */
  @Column(name = "count", nullable = false)
  private Integer count;

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
   * 线路id
   */
  @Column(name = "roadid", nullable = false)
  private Integer roadid;

  /**
   * 旅游id
   */
  @Column(name = "travelid", nullable = false)
  private Integer travelid;

}
