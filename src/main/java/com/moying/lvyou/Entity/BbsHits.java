package com.moying.lvyou.Entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * 帖子查看
 */

@Data
@Entity(name = "BbsHits")
@Table(name = "bbs_hits")
public class BbsHits {
  /**
   * 自增ID
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "bhid", nullable = false)
  private Integer cid;

  /**
   * 帖子ID
   */
  @Column(name = "bbsid", nullable = false)
  private Integer bbsid;

  /**
   * 用户ID
   */
  @Column(name = "userid", nullable = false)
  private Integer userid;

  /**
   * 查看时间
   */
  private Date createtime;

}
