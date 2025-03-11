package com.moying.lvyou.Entity;

import java.util.Date;

import lombok.Data;

import javax.persistence.*;

/*
 * 活动表
 */

@Data
@Entity(name = "Activities")
@Table(name = "activities")
public class Activities {
    /**
     * 自增ID
     */
    @Column(name = "aid")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer aid;

    /**
     * 用户id
     */
    @Column(name = "userid")
    private Integer userid;

    /**
     * 标题
     */
    @Column(name = "title")
    private String title;

    /**
     * 标题图片
     */
    @Column(name = "pic")
    private String pic;

    /**
     * 图片列表
     */
    @Column(name = "piclist")
    private String piclist;

    /**
     * 内容
     */
    @Column(name = "content")
    private String content;

    /**
     * 是否锁定
     */
    @Column(name = "islock")
    private byte islock;

    /**
     * 锁定可查看用户列表
     */
    @Column(name = "lockuser")
    private String lockuser;

    /**
     * 发布时间
     */
    @Column(name = "createtime")
    private Date createtime;
}
