package cn.hulingfeng.controller;

import cn.hulingfeng.service.DocService;
import cn.hulingfeng.service.ESService;
import org.apache.logging.log4j.core.util.FileUtils;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 爬取网易新闻当天热点新闻
 * @author hlf
 * @title: NewsController
 * @projectName es
 * @description: TODO
 * @date 2020/3/19 11:58
 */
@RestController
public class NewsController {

    @Autowired
    private ESService esService;
    @Autowired
    private DocService docService;
    @Autowired
    private RestHighLevelClient client;

    private static final Logger log = LoggerFactory.getLogger(NewsController.class);


    private static Pattern URL_PATTERN = Pattern.compile("https://news.163.com/(\\d{2}/\\d{4}/\\d{2}/\\w+).html");
    private static Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");

    /**
     * 获取热点新闻链接，展示用
     * @return
     * @throws Exception
     */
    @GetMapping("get_top_news_url")
    public List<String> fetch163TopNewsUrl() {
        String newswebsite = "https://news.163.com/";
        List<String> urlList = new ArrayList<>();
        Document document = null;
        try {
            document = Jsoup.connect(newswebsite).validateTLSCertificates(false).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element topNews = document.getElementById("js_top_news");
        Elements elements = topNews.select("a");
        for (Element a : elements) {
            //过滤只属于网易新闻的文章
            if(a.attr("href").startsWith("https://news.163.com/20")){
                urlList.add(a.attr("href"));
            }
        }
        return urlList;
    }

    /**
     * 获取热点新闻链接并解析
     * @throws IOException
     * @throws ParseException
     */
    @GetMapping("fetch_news")
    public List<ResponseEntity> fetch163TopNews() {
        String newswebsite = "https://news.163.com/";
        List<ResponseEntity> responseEntityList = new ArrayList<>();
        List<String> urlList = new ArrayList<>();
        Document document = null;
        try {
            document = Jsoup.connect(newswebsite).validateTLSCertificates(false).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element topNews = document.getElementById("js_top_news");
        Elements elements = topNews.select("a");
        //确保es连接
        try {
            if(this.client.ping(RequestOptions.DEFAULT)){
                for (Element a : elements) {
                    //过滤只属于网易新闻的文章,避免不同页面格式造成解析错误
                    if(a.attr("href").startsWith("https://news.163.com/20")){
                        ResponseEntity responseEntity = fetch163News(a.attr("href"));
                        urlList.add(a.attr("href"));
                        responseEntityList.add(responseEntity);
                    }
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return responseEntityList;
    }

    /**
     * 解析单篇新闻
     * @param url
     * @throws IOException
     * @throws ParseException
     */
    public ResponseEntity fetch163News(String url) throws IOException, ParseException {
        //创建要写入的文件,文件名默认当前系统时间戳
        String fileName = System.currentTimeMillis()+".txt";
        Matcher matcher = URL_PATTERN.matcher(url);
        if(matcher.find()) {
            fileName = matcher.group(1).replace("/","")+".txt";
        }
        String path = URLDecoder.decode(getClass().getResource("/doc").getPath(),"utf-8");
        File file = new File(path+fileName);
        if(file.exists()){
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),"utf-8"));
        //解析网页新闻文档
        Document document = Jsoup.connect(url).validateTLSCertificates(false).get();
        Element postContentMain = document.getElementsByClass("post_content_main").first();
        //新闻标题
        Element h1 = postContentMain.getElementsByTag("h1").first();
        //新闻标题单独成行
        bufferedWriter.write(h1.text());
        bufferedWriter.newLine();
        //发行时间解析
        Element postTimeSource = document.getElementsByClass("post_time_source").first();
        Matcher dateMatcher = DATE_PATTERN.matcher(postTimeSource.text());
        String dateTime = "";
        if(dateMatcher.find()){
            dateTime = dateMatcher.group();
        }
        //新闻来源
        Element a = postTimeSource.getElementsByTag("a").first();
        //新闻发行时间和来源单独成行
        bufferedWriter.write(postTimeSource.text());
        bufferedWriter.newLine();
        //新闻文本内容
        String content = "";
        Element postText = document.getElementsByClass("post_text").first();
        Elements elements = postText.getElementsByTag("p");
        for(Element p : elements){
            content += p.text();
            //每段新闻内容成行
            if(!p.text().equals("")) {
                bufferedWriter.write(p.text());
                bufferedWriter.newLine();
            }
        }
        //责任编辑
        Element epSource = postText.getElementsByClass("ep-source cDGray").first();
        //新闻来源和责任编辑单独成行
        bufferedWriter.write(epSource.getElementsByTag("span").get(0).text()+" "+epSource.getElementsByTag("span").get(1).text());
        //新闻责任编辑
        Element span = epSource.getElementsByTag("span").get(1);
        String[] epEditor = span.text().split("\\：");
        //拼接新闻字段
        String title = h1.text();
        String publishDate = dateTime;
        String source = a.text();
        String editor = epEditor[1];
        String desc = elements.get(1).text();
        //文本内容
        bufferedWriter.flush();
        bufferedWriter.close();
        String featureWords = docService.filterKeywords(fileName);
        //文件信息导入到elasticsearch
        return esService.add(title,source,editor,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(publishDate),desc,fileName,content.length(),featureWords);
    }
}
