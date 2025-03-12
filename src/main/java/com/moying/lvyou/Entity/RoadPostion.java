package com.moying.lvyou.Entity;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

import javax.persistence.*;

/**
 * 线路景点信息表
 */

@Data
@Entity(name = "RoadPostion")
@Table(name = "road_postion")
public class RoadPostion {
  /**
   * 自增ID
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "rpid", nullable = false)
  private Integer rpid;

  /**
   * 线路id
   */
  @Column(name = "rid", nullable = false)
  private Integer rid;

  /**
   * 景点id
   */
  @Column(name = "pid", nullable = false)
  private Integer pid;

  /**
   * 编号
   */
  @Column(name = "sn", nullable = false)
  private Integer sn;

  /**
   * 距离上一个景点的路程(米)
   */
  @Column(name = "distance", nullable = false)
  private Integer distance;

  /**
   * 在该景点打卡积分
   */
  @Column(name = "point", nullable = false)
  private Integer point;

  /**
   * 是否显示
   */
  @Column(name = "isshow", nullable = false)
  private Byte isshow;

  /**
   * 创建时间
   */
  @Column(name = "createtime", nullable = false)
  private Date createtime;

  /**
   * 点亮点横坐标
   */
  @Column(name = "cur_x", nullable = false, precision = 10, scale = 2)
  private BigDecimal curX;

  /**
   * 点亮点纵坐标
   */
  @Column(name = "cur_y", nullable = false, precision = 10, scale = 2)
  private BigDecimal curY;

}
