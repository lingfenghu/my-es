package cn.hulingfeng.entity;

import java.util.Date;

/**
 * @author hlf
 * @title: MysqlNews
 * @projectName es
 * @description: TODO
 * @date 2020/2/26 14:33
 */
public class MysqlNews {
    private Integer id;
    private String title;
    private String author;
    private String content;
    private Date publish_date;
    private Date update_time;

    public MysqlNews(Integer id, String title, String author, String content, Date publish_date, Date update_time) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.content = content;
        this.publish_date = publish_date;
        this.update_time = update_time;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getPublish_date() {
        return publish_date;
    }

    public void setPublish_date(Date publish_date) {
        this.publish_date = publish_date;
    }

    public Date getUpdate_time() {
        return update_time;
    }

    public void setUpdate_time(Date update_time) {
        this.update_time = update_time;
    }
}
