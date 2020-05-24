# Elasticsearch的简单应用,新闻文档简单过滤和管理，nginx日志集中管理
### 预览图片在raw文件夹下。
### 注意，该项目有两个分支，主要是因为涉及文件存储而区分，deploy下clone到本地后，打包后可直接在tomcat启动运行，当然首先要确保Elasticsearch连接成功
### 预览图片在raw文件夹下。
#### 本地运行测试，文件上传下载路径应为：src/main/resources/doc/
#### 打包部署运行，文件上传下载路径应为：classpath:doc/
下面演示部署启动项目
1. 配置elasticsearch，logstash,nginx
elasticsearch.yml
-----------------
#解决跨域
http.cors.enabled: true
http.cors.allow-origin: "*"
#
cluster.name: hulingfeng
node.name: master
node.master: true
#
network.host: 127.0.0.1
http.port: 9200
-----------------

2. 启动elasticsearch  `./bin/elasticsearch.bat`  ,启动logstash 带配置nginx-pipeline2.conf启动,启动nginx
3. 启动tomcat
4. 访问http://localhost:8080/dist
5. 项目部分页面预览：
 ![主页](https://github.com/lingfenghu/uni_project_java/blob/master/raw/1.png)
 ![文档管理](https://github.com/lingfenghu/uni_project_java/blob/master/raw/2.png)
 ![日志管理](https://github.com/lingfenghu/uni_project_java/blob/master/raw/3.png)

### 欢迎参观
* [个人主页](https://lingfenghu.github.io/)
