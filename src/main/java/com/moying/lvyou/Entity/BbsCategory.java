package com.moying.lvyou.Entity;

import java.util.Date;

import lombok.Data;

import javax.persistence.*;

/**
 * 帖子分类
 */

@Data
@Entity(name = "BbsCategory")
@Table(name = "bbs_category")
public class BbsCategory {
  /**
   * 自增ID
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "cid", nullable = false)
  private Integer cid;

  /**
   * 类别名称
   */
  private String name;

  /**
   * 标题图片
   */
  private String pic;

  /**
   * 发布时间
   */
  private Date createtime;

  /**
   * 是否显示
   */
  private Byte isshow;
}
