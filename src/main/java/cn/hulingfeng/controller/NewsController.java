package cn.hulingfeng.controller;

import cn.hulingfeng.service.ESService;
import cn.hulingfeng.utils.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

    /**
     * 获取所有热点新闻链接
     * @throws IOException
     * @throws ParseException
     */
    @GetMapping("fetch_news")
    public void fetch163TopNewsUrl() throws Exception {
        String newswebsite = "https://news.163.com/";
        List<ResponseEntity> responseEntityList = new ArrayList<>();
        Document document = Jsoup.connect(newswebsite).validateTLSCertificates(false).get();
        Element topNews = document.getElementById("js_top_news");
        Elements elements = topNews.select("a");
        for (Element a : elements) {
            ResponseEntity responseEntity = fetch163News(a.attr("href"));
            responseEntityList.add(responseEntity);
        }
    }

    /**
     * 解析单篇新闻
     * @param url
     * @throws IOException
     * @throws ParseException
     */
    public ResponseEntity fetch163News(String url) throws IOException, ParseException {
        Document document = Jsoup.connect(url).get();
        //获取新闻内容
        Element postContentMain = document.getElementsByClass("post_content_main").first();
        //新闻标题
        Element h1 = postContentMain.getElementsByTag("h1").first();
        //发行时间解析
        Element postTimeSource = document.getElementsByClass("post_time_source").first();
        Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        Matcher matcher = pattern.matcher(postTimeSource.text());
        String dateTime = "";
        if(matcher.find()){
            dateTime = matcher.group();
        }
        //新闻来源
        Element a = postTimeSource.getElementsByTag("a").first();
        //新闻文本
        String article = "";
        Element postText = document.getElementsByClass("post_text").first();
        Elements elements = postText.getElementsByTag("p");
        for(Element p : elements){
            article += p.text();
        }
        //责任编辑
        Element epSource = postText.getElementsByClass("ep-source cDGray").first();
        Element span = epSource.getElementsByTag("span").get(1);
        String[] epEditor = span.text().split("\\：");
        //拼接新闻字段
        String title = h1.text();
        String publishDate = dateTime;
        String source = a.text();
        String editor = epEditor[1];
        String desc = elements.get(1).text();
        //文本内容
        String content = article;

        String news = title+"||"+dateTime+"||"+source+"||"+editor+"||"+content;
        //写入文件
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(news.getBytes());
        String fileName = System.currentTimeMillis()+".txt";
        File file = new File(FileUtils.PATH+fileName);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(news.getBytes());
        fileOutputStream.close();
        //导入到elasticsearch
        return esService.add(title,source,editor,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(publishDate),desc,fileName,content.length());
    }
}
