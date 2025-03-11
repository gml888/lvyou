package com.moying.lvyou.Entity;

import java.util.Date;

import lombok.Data;

import javax.persistence.*;

/**
 * bbs表
 */
@Data
@Entity(name = "Bbs")
@Table(name = "bbs")
public class Bbs {
  /**
   * 自增ID
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "bid", nullable = false)
  private Integer bid;

  /**
   * 类别id
   */
  @Column(name = "cid", nullable = false)
  private Integer cid;

  /**
   * 用户id
   */
  @Column(name = "userid", nullable = false)
  private Integer userid;

  /**
   * 标题
   */
  @Column(name = "title", nullable = false, length = 100)
  private String title;

  /**
   * 标题图片
   */
  @Column(name = "pic", nullable = false, length = 200)
  private String pic;

  /**
   * 图片列表
   */
  @Column(name = "piclist", nullable = false, length = 2000)
  private String piclist;

  /**
   * 内容
   */
  @Lob
  @Column(name = "content")
  private String content;

  /**
   * 发布时间
   */
  @Column(name = "createtime", nullable = false)
  private Date createtime;

  /**
   * 是否显示
   */
  @Column(name = "isshow", nullable = false)
  private byte isshow;
}
