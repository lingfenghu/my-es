package cn.hulingfeng.service;

import cn.hulingfeng.controller.ESController;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ESService {

    @Autowired
    private RestHighLevelClient client;

    /**
     *
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
