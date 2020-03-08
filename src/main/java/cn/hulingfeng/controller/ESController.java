package cn.hulingfeng.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.search.MultiMatchQuery;
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
     * 由于涉及分页，searchResponse返回的东西比较杂，于是将查询结果和总条数提取出来，再转换成json格式返回给前端
     */
    private ObjectMapper objectMapper = new ObjectMapper();

    private static final Integer DEFAULT_PAGE_SIZE = 10;

    /**
     * 创建索引
     * @param indexName
     * @return
     * @throws IOException
     */
    @PostMapping("/index/create")
    public ResponseEntity createIndex(@RequestParam(name = "indexName", defaultValue = "news") String indexName) {
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        request.settings(Settings.builder()
                .put("index.number_of_shards", 5)
                .put("index.number_of_replicas", 0)
        );

        //mapping构建
        XContentBuilder builder = null;
        CreateIndexResponse createIndexResponse = null;

        try {
            builder = XContentFactory.jsonBuilder().startObject()
                    .startObject("properties")
                        .startObject("type")//文档类型
                        .field("type", "keyword")
                        .endObject()
                        .startObject("file_name")//文件名
                        .field("type", "text")
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
                        .startObject("content")//内容
                        .field("type", "text")
                        .endObject()
                        .startObject("word_count")//总字数
                        .field("type", "integer")
                        .endObject()
                    .endObject()
            .endObject();
            request.mapping(builder);

            createIndexResponse = this.client.indices().create(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }

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
        GetRequest getRequest = new GetRequest("news", id);
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
            @RequestParam(name = "type") String type,
            @RequestParam(name = "file_name") String fileName,
            @RequestParam(name = "title") String title,
            @RequestParam(name = "author") String author,
            @RequestParam(name = "publish_date")
                    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date publishDate,
            @RequestParam(name = "content") String content,

            @RequestParam(name = "word_count") Integer wordCount) {
        try {
            XContentBuilder xContent = XContentFactory.jsonBuilder()
                    .startObject()
                    .field("title", title)
                    .field("file_name", fileName)
                    .field("author", author)
                    .field("type", type)
                    .field("publish_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(publishDate))
                    .field("content", content)
                    .field("word_count", wordCount)
                    .endObject();
            IndexRequest request = new IndexRequest("news").source(xContent);
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
        DeleteRequest request = new DeleteRequest("news", id);
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
            UpdateRequest request = new UpdateRequest("news", id).doc(builder);
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
    @GetMapping("/query/document")
    public ResponseEntity query(
            @RequestParam(name = "file_name",required = false)String fileName,
            @RequestParam(name = "author",required = false)String author,
            @RequestParam(name = "title",required = false)String title,
            @RequestParam(name = "type",required = false)String type,
            @RequestParam(name = "gt_word_count",defaultValue = "0")Integer gtWordCount,
            @RequestParam(name = "lt_word_count",required = false)Integer ltWordCount,
            @RequestParam(name = "gt_publish_date",required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd") Date gtPublishDate,
            @RequestParam(name = "lt_publish_date",required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd")Date ltPublishDate,
            @RequestParam(name = "page_num",defaultValue = "1")Integer pageNum) {

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if(fileName != null){
            boolQuery.must(QueryBuilders.matchQuery("file_name",fileName));
        }
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
        //将结束时间加一天
        if(ltPublishDate != null){
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(ltPublishDate);
            calendar.add(Calendar.DAY_OF_MONTH,1);
            dateRangeQuery.to(calendar.getTimeInMillis());
        }
        boolQuery.filter(dateRangeQuery);

        SearchSourceBuilder builder = new SearchSourceBuilder().query(boolQuery).from((pageNum-1)*DEFAULT_PAGE_SIZE).size(DEFAULT_PAGE_SIZE);

        SearchRequest searchRequest = new SearchRequest("news").source(builder);
        SearchResponse searchResponse = null;
        try {
             searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Map result = new HashMap();
        result.put("total",searchResponse.getHits().getTotalHits().value);
        List<Map<String,Object>> list = new ArrayList<>();
        for(SearchHit hit : searchResponse.getHits()){
            list.add(hit.getSourceAsMap());
        }
        result.put("list",list);
        try {
            return new ResponseEntity(objectMapper.writeValueAsString(result),HttpStatus.OK);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 搜索框搜索
     * @param queryContent
     * @param pageNum
     * @return
     * @throws JsonProcessingException
     */
    @GetMapping("/query/global/document")
    public ResponseEntity query(
            @RequestParam(name = "query_content",defaultValue = "")String queryContent,
            @RequestParam(name = "page_num",defaultValue = "1")Integer pageNum) {
        String fieldName1 = "title";
        String fieldName2 = "author";
        String fieldName3 = "content";
        MultiMatchQueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(queryContent,fieldName1,fieldName2,fieldName3);
        SearchSourceBuilder builder = new SearchSourceBuilder().query(multiMatchQuery).from((pageNum-1)*DEFAULT_PAGE_SIZE).size(DEFAULT_PAGE_SIZE);

        SearchRequest searchRequest = new SearchRequest("news").source(builder);
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Map result = new HashMap();
        result.put("total",searchResponse.getHits().getTotalHits().value);
        List<Map<String,Object>> list = new ArrayList<>();
        for(SearchHit hit : searchResponse.getHits()){
            list.add(hit.getSourceAsMap());
        }
        result.put("list",list);
        try {
            return new ResponseEntity(objectMapper.writeValueAsString(result),HttpStatus.OK);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
