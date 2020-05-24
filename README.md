# Elasticsearch的简单应用,新闻文档简单过滤和管理，nginx日志集中管理
后端框架：SpringBoot整合Elasticsearch客户端<br>
前端：Vue + axios + element<br>
具体实现：手动上传txt新闻文档到ES节点，也可通过爬取网易热点新闻抓取并分析得到txt新闻文本，对文本内容出现频率大于5次的词作为文档特征搜索词，实现对文档的日期、字数范围搜索，标题、编辑、简述、来源关键字搜索,文档并可下载，对nginx日志实现实时收集，可导出excel
### 预览图片在raw文件夹下。
## my-es 基于Elasticsearch和文本过滤的文档管理终端 [前端部分](https://github.com/lingfenghu/my-vue)
### 项目clone到本地后，打包成war后可直接在tomcat启动运行，当然首先要确保Elasticsearch连接成功
### 预览图片在raw文件夹下。
### es和logstash配置文件在elk_config文件夹下
#### 本地运行测试，文件上传下载路径应为：src/main/resources/doc/
#### 打包部署运行，文件上传下载路径应为：classpath:doc/
下面演示部署启动项目
1. 配置elasticsearch，logstash,nginx
2. 启动elasticsearch  `./bin/elasticsearch.bat`,带配置nginx-pipeline.conf启动logstash `./bin/logstash -f nginx-pipeline2.conf`
3. 启动tomcat
4. 访问http://localhost:8080/dist
5. 项目部分页面预览：
 ![文档获取](https://github.com/lingfenghu/my-es/blob/master/raw/1.png)
 ![日志管理](https://github.com/lingfenghu/my-es/blob/master/raw/2.png)
 ![新闻获取](https://github.com/lingfenghu/my-es/blob/master/raw/3.png)
 ![文档上传](https://github.com/lingfenghu/my-es/blob/master/raw/4.png)
### 欢迎参观
* [个人主页](https://lingfenghu.github.io/)
