package com.moying.lvyou.Entity;

import java.util.Date;

import lombok.Data;

import javax.persistence.*;

/**
 * 景点图片信息表
 */

@Data
@Entity(name = "PostionPic")
@Table(name = "postion_pic")
public class PostionPic {
  /**
   * 自增ID
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "picid", nullable = false)
  private Integer picid;

  /**
   * 景点id
   */
  @Column(name = "pid", nullable = false)
  private Integer pid;

  /**
   * 图片名称
   */
  @Column(name = "title", nullable = false)
  private String title;

  /**
   * 图片路径
   */
  @Column(name = "pic", nullable = false)
  private String pic;

  /**
   * 编号
   */
  @Column(name = "sn", nullable = false)
  private Integer sn;

  /**
   * 创建时间
   */
  @Column(name = "createtime", nullable = false)
  private Date createtime;
}
