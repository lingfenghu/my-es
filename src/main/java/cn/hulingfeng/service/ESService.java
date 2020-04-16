package cn.hulingfeng.service;

import cn.hulingfeng.controller.ESController;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author hlf
 * @title: ESService
 * @projectName es
 * @description: TODO
 * @date 2020/3/20 16:05
 */
@Service
public class ESService implements ApplicationRunner {

    @Autowired
    private RestHighLevelClient client;

    public static final  String NEWS_DOCUMENT_INDEX = "news-doc";

    /**
     * 启动自动创建文档索引
     * @param args
     * @throws Exception
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        GetIndexRequest request = new GetIndexRequest(NEWS_DOCUMENT_INDEX);
        boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
        if(!exists){
            createIndex(NEWS_DOCUMENT_INDEX);
        }
    }

    /**
     * 创建新闻文档索引
     * @param indexName
     * @return
     * @throws IOException
     */
    public void createIndex(String indexName) {
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        request.settings(Settings.builder()
                .put("index.number_of_shards", 5)
                .put("index.number_of_replicas", 1)
        );
        //mapping构建
        XContentBuilder builder = null;
        CreateIndexResponse createIndexResponse = null;
        try {
            builder = XContentFactory.jsonBuilder().startObject()
                    .startObject("properties")
                    .startObject("title")//标题
                    .field("type", "text")
                    .field("analyzer","ik_max_word")
                    .endObject()
                    .startObject("publish_date")//发表日期
                    .field("type", "date")
                    .field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis")
                    .endObject()
                    .startObject("source")//来源
                    .field("type", "text")
                    .field("analyzer","ik_max_word")
                    .field("search_analyzer","ik_smart")
                    .endObject()
                    .startObject("editor")//责任编辑
                    .field("type", "text")
                    .field("analyzer","ik_max_word")
                    .endObject()
                    .startObject("desc")//简述
                    .field("type", "text")
                    .field("analyzer","ik_max_word")
                    .endObject()
                    .startObject("file_name")//文件名
                    .field("type", "keyword")
                    .endObject()
                    .startObject("word_count")//总字数
                    .field("type", "integer")
                    .endObject()
                    .startObject("feature_words")//特征关键字
                    .field("type", "text")
                    .field("analyzer","whitespace")
                    .endObject()
                    .endObject()
                    .endObject();
            request.mapping(builder);
            this.client.indices().create(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 添加文档
     * @param title
     * @param source
     * @param editor
     * @param publishDate
     * @param desc
     * @param fileName
     * @param wordCount
     * @return
     */
    public ResponseEntity add(String title, String source, String editor, Date publishDate,
                              String desc, String fileName, Integer wordCount, String featureWords){
        try {
            XContentBuilder xContent = XContentFactory.jsonBuilder()
                    .startObject()
                    .field("title", title)
                    .field("source", source)
                    .field("editor", editor)
                    .field("publish_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(publishDate))
                    .field("desc", desc)
                    .field("file_name", fileName)
                    .field("word_count", wordCount)
                    .field("feature_words", featureWords)
                    .endObject();
            IndexRequest request = new IndexRequest(ESController.NEWS_DOCUMENT_INDEX).source(xContent);
            IndexResponse result = this.client.index(request, RequestOptions.DEFAULT);
            return new ResponseEntity(result.getId(), HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
