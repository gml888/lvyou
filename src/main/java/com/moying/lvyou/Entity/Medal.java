package com.moying.lvyou.Entity;

import java.util.Date;

import lombok.Data;

import javax.persistence.*;

/**
 * 勋章表
 */
@Data
@Entity(name = "Medal")
@Table(name = "medal")
public class Medal {
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
   * 用户id
   */
  @Column(name = "userid", nullable = false)
  private Integer userid;

  /**
   * 勋章图片
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
