package com.moying.lvyou.Entity;

import java.util.Date;

import lombok.Data;

import javax.persistence.*;

/**
 * 回复信息表
 */

@Data
@Entity(name = "Reply")
@Table(name = "reply")
public class Reply {
  /**
   * 自增ID
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "rid", nullable = false)
  private Integer rid;

  /**
   * 回复用户
   */
  @Column(name = "uid", nullable = false)
  private Integer uid;

  /**
   * 类别:1.bbs2.活动
   */
  @Column(name = "kind", nullable = false)
  private Byte kind;

  /**
   * 所回复信息的id
   */
  @Column(name = "infoid", nullable = false)
  private Integer infoid;

  /**
   * 内容
   */
  @Column(name = "content", nullable = false)
  private String content;

  /**
   * 点赞数
   */
  @Column(name = "zans", nullable = false)
  private Integer zans;

  /**
   * 点赞用户列表[{uid:1,time:'2025-2-12 14:30'}]
   */
  @Column(name = "zanlist", nullable = false)
  private String zanlist;

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
