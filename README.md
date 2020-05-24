# my-es 
### 预览图片在raw文件夹下。
### 注意，该项目有两个分支，主要是因为涉及文件存储而区分，deploy下clone到本地后，打包后可直接在tomcat启动运行，当然首先要确保Elasticsearch连接成功
### 预览图片在raw文件夹下。
#### 本地运行测试，文件上传下载路径应为：src/main/resources/doc/
#### 打包部署运行，文件上传下载路径应为：classpath:doc/
下面演示部署启动项目
1.启动elasticsearch,启动logstash 带配置nginx-pipeline2.conf启动,启动nginx
2.启动tomcat
3.访问http://localhost:8080/dist
4. 项目部分页面预览：
 ![主页](https://github.com/lingfenghu/uni_project_java/blob/master/raw/1.png)
 ![文档管理](https://github.com/lingfenghu/uni_project_java/blob/master/raw/2.png)
 ![日志管理](https://github.com/lingfenghu/uni_project_java/blob/master/raw/3.png)

### 欢迎参观
* [个人主页](https://lingfenghu.github.io/)
