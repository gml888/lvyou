package com.moying.lvyou.Entity;

import java.util.Date;

import lombok.Data;

import javax.persistence.*;

/**
 * 线路景点打卡选项表
 */

@Data
@Entity(name = "RoadPostionOption")
@Table(name = "road_postion_option")
public class RoadPostionOption {
  /**
   * 自增ID
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "rpoid", nullable = false)
  private Integer rpoid;

  /**
   * 线路景点id
   */
  @Column(name = "rpid", nullable = false)
  private Integer rpid;

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
   * 内容
   */
  @Lob
  @Column(name = "content")
  private String content;

  /**
   * 序号
   */
  @Column(name = "sn", nullable = false)
  private Integer sn;

  /**
   * 跳转地址
   */
  @Column(name = "url", nullable = false)
  private String url;

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
