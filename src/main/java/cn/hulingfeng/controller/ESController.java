package cn.hulingfeng.controller;

import org.apache.lucene.util.QueryBuilder;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author hlf
 * @title: ESController
 * @projectName es
 * @description: TODO
 * @date 2020/2/18 20:38
 */
@RestController
public class ESController {

    @Autowired
    private RestHighLevelClient client;

    /**
     * 创建索引
     *
     * @param indexName
     * @return
     * @throws IOException
     */
    @PostMapping("/index/create")
    public ResponseEntity createIndex(@RequestParam(name = "indexName", defaultValue = "document") String indexName) throws IOException {
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        request.settings(Settings.builder()
                .put("index.number_of_shards", 5)
                .put("index.number_of_replicas", 0)
        );

        //mapping构建
        XContentBuilder builder = XContentFactory.jsonBuilder().startObject()
                .startObject("properties")
                    .startObject("type")//文档类型
                    .field("type", "keyword")
                    .endObject()
                    .startObject("title")//标题
                    .field("type", "text")
                    .endObject()
                    .startObject("author")//作者
                    .field("type", "keyword")
                    .endObject()
                    .startObject("publish_date")//发表日期
                    .field("type", "date")
                    .field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis")
                    .endObject()
                    .startObject("word_count")//总字数
                    .field("type", "integer")
                    .endObject()
                .endObject()
        .endObject();
        request.mapping(builder);

        CreateIndexResponse createIndexResponse = this.client.indices().create(request, RequestOptions.DEFAULT);
        boolean acknowledged = createIndexResponse.isAcknowledged();
        if (acknowledged) {
            return new ResponseEntity(HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 通过id获取文档
     * @param id
     * @return
     */
    @GetMapping("/get/document")
    public ResponseEntity get(@RequestParam(name = "id", defaultValue = "") String id) {
        GetRequest getRequest = new GetRequest("document", id);
        GetResponse result = null;
        if (id.isEmpty()) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        try {
            result = client.get(getRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (!result.isExists()) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity(result.getSource(), HttpStatus.OK);
    }

    /**
     * 添加文档
     * @param title
     * @param author
     * @param type
     * @param publishDate
     * @param wordCount
     * @return
     */
    @PostMapping("add/document")
    public ResponseEntity add(
            @RequestParam(name = "title") String title,
            @RequestParam(name = "author") String author,
            @RequestParam(name = "type") String type,
            @RequestParam(name = "publish_date")
                    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date publishDate,
            @RequestParam(name = "word_count") Integer wordCount) {
        try {
            XContentBuilder content = XContentFactory.jsonBuilder()
                    .startObject()
                    .field("title", title)
                    .field("author", author)
                    .field("type", type)
                    .field("publish_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(publishDate))
                    .field("word_count", wordCount)
                    .endObject();
            IndexRequest request = new IndexRequest("document").source(content);
            IndexResponse result = this.client.index(request, RequestOptions.DEFAULT);
            return new ResponseEntity(result.getId(), HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 更新文档
     * @param id
     * @return
     */
    @DeleteMapping("/delete/document")
    public ResponseEntity delete(@RequestParam(name = "id")String id){
        DeleteRequest request = new DeleteRequest("document", id);
        DeleteResponse result = null;
        try {
            result = client.delete(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity(result.getResult(),HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 修改文档
     * @param id
     * @param title
     * @param author
     * @param type
     * @param publishDate
     * @param wordCount
     * @return
     */
    @PutMapping("/update/document")
    public ResponseEntity update(
            @RequestParam(name = "id")String id,
            @RequestParam(name = "title",required = false) String title,
            @RequestParam(name = "author",required = false) String author,
            @RequestParam(name = "type",required = false) String type,
            @RequestParam(name = "publish_date",required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date publishDate,
            @RequestParam(name = "word_count",required = false) Integer wordCount){
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder().startObject();
            if(title != null && !title.isEmpty()){
                builder.field("title", title);
            }
            if(author != null && !author.isEmpty()){
                builder.field("author", author);
            }
            if(type != null && !type.isEmpty()){
                builder.field("type", type);
            }
            if(publishDate != null){
                builder.field("publish_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(publishDate));
            }
            if(wordCount != null && wordCount > 0){
                builder.field("word_count", wordCount);
            }
            builder.endObject();
            UpdateRequest request = new UpdateRequest("document", id).doc(builder);
            UpdateResponse result = client.update(request, RequestOptions.DEFAULT);
            return new ResponseEntity(result.getResult().toString(),HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 文档查询
     * @param author
     * @param title
     * @param type
     * @param gtWordCount
     * @param ltWordCount
     * @param gtPublishDate
     * @param ltPublishDate
     * @return
     */
    @PostMapping("/query/document")
    public ResponseEntity query(
            @RequestParam(name = "author",required = false)String author,
            @RequestParam(name = "title",required = false)String title,
            @RequestParam(name = "type",required = false)String type,
            @RequestParam(name = "gt_word_count",defaultValue = "0")Integer gtWordCount,
            @RequestParam(name = "lt_word_count",required = false)Integer ltWordCount,
            @RequestParam(name = "gt_publish_date",required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date gtPublishDate,
            @RequestParam(name = "lt_publish_date",required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")Date ltPublishDate){

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if(author != null){
            boolQuery.must(QueryBuilders.matchQuery("author",author));
        }
        if(title != null){
            boolQuery.must(QueryBuilders.matchQuery("title",title));
        }
        if(type != null){
            boolQuery.must(QueryBuilders.matchQuery("type",type));
        }
        RangeQueryBuilder wordCountRangeQuery = QueryBuilders.rangeQuery("word_count").from(gtWordCount);
        if(ltWordCount != null && ltWordCount > 0){
            wordCountRangeQuery.to(ltWordCount);
        }
        boolQuery.filter(wordCountRangeQuery);

        RangeQueryBuilder dateRangeQuery = QueryBuilders.rangeQuery("publish_date");
        if(gtPublishDate != null){
            dateRangeQuery.from(gtPublishDate.getTime());
        }
        if(ltPublishDate != null){
            dateRangeQuery.to(ltPublishDate.getTime());
        }
        boolQuery.filter(dateRangeQuery);

        SearchSourceBuilder builder = new SearchSourceBuilder().query(boolQuery).from(0).size(10);

        SearchRequest searchRequest = new SearchRequest("document").source(builder);
        SearchResponse searchResponse = null;
        try {
             searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        List<Map<String,Object>> result = new ArrayList<>();
        for(SearchHit hit : searchResponse.getHits()){
            result.add(hit.getSourceAsMap());
        }
        return new ResponseEntity(result,HttpStatus.OK);
    }

}
