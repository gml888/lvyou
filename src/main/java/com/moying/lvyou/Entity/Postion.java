package com.moying.lvyou.Entity;

import java.util.Date;

import lombok.Data;

import javax.persistence.*;

/**
 * 景点信息表
 */

@Data
@Entity(name = "Postion")
@Table(name = "postion")
public class Postion {
  /**
   * 自增ID
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "pid", nullable = false)
  private Integer pid;

  /**
   * 名称
   */
  @Column(name = "name", nullable = false, length = 50)
  private String name;

  /**
   * 维度
   */
  @Column(name = "lat", nullable = false, length = 20)
  private String lat;

  /**
   * 经度
   */
  @Column(name = "lng", nullable = false, length = 20)
  private String lng;

  /**
   * 打卡距离
   */
  @Column(name = "range", nullable = false)
  private Integer range;

  /**
   * 打卡积分
   */
  @Column(name = "points", nullable = false)
  private Integer points;

  /**
   * 景点介绍
   */
  @Lob
  @Column(name = "content")
  private String content;

  /**
   * 景点细节
   */
  @Lob
  @Column(name = "detail")
  private String detail;

  /**
   * 创建时间
   */
  @Column(name = "createtime", nullable = false)
  private Date createtime;

  /**
   * 景点打卡选项图片
   */
  @Column(name = "option_pic", nullable = false)
  private String optionPic;

  /**
   * 景点打卡选项说明
   */
  @Column(name = "option_content", nullable = false, length = 5000)
  private String optionContent;

}
