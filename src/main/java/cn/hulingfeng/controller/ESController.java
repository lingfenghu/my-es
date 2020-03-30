package cn.hulingfeng.controller;

import cn.hulingfeng.service.ESService;
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

    @Autowired
    private ESService esService;

    public static final  String NEWS_DOCUMENT_INDEX = "news-doc";

    private static final Integer DEFAULT_PAGE_SIZE = 10;

    //由于涉及分页，searchResponse返回的东西比较杂，于是将查询结果和总条数提取出来，再转换成json格式返回给前端
    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建文档索引
     * @param indexName
     * @return
     * @throws IOException
     */
    @PostMapping("/index/create")
    public ResponseEntity createIndex(@RequestParam(name = "indexName", defaultValue = NEWS_DOCUMENT_INDEX) String indexName) {
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
    @Deprecated
    @GetMapping("/get/document")//推荐@PathVariable
    public ResponseEntity get(@RequestParam(name = "id", defaultValue = "") String id) {
        GetRequest getRequest = new GetRequest("news", id);
        GetResponse result;
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
     * @param source
     * @param editor
     * @param publishDate
     * @param desc
     * @param fileName
     * @param wordCount
     * @return
     */
    @Deprecated
    @PostMapping("add/document")
    public ResponseEntity add(
            @RequestParam(name = "title") String title,
            @RequestParam(name = "source") String source,
            @RequestParam(name = "editor") String editor,
            @RequestParam(name = "publish_date")
                    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date publishDate,
            @RequestParam(name = "desc") String desc,
            @RequestParam(name = "file_name") String fileName,
            @RequestParam(name = "word_count") Integer wordCount,
            @RequestParam(name = "feature_words")String featureWords) {
        return esService.add(title,source,editor,publishDate,desc,fileName,wordCount,featureWords);
    }

    /**
     * 删除文档
     * @param id
     * @return
     */
    @Deprecated
    @DeleteMapping("/delete/document")//推荐@PathVariable
    public ResponseEntity delete(@RequestParam(name = "id")String id){
        DeleteRequest request = new DeleteRequest("news", id);
        DeleteResponse result;
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
     * @param source
     * @param editor
     * @param publishDate
     * @param desc
     * @return
     */
    @Deprecated
    @PutMapping("/update/document")
    public ResponseEntity update(
            @RequestParam(name = "id")String id,
            @RequestParam(name = "title") String title,
            @RequestParam(name = "source") String source,
            @RequestParam(name = "editor") String editor,
            @RequestParam(name = "publish_date")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date publishDate,
            @RequestParam(name = "desc") String desc){
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder().startObject();
            if(title != null && !title.isEmpty()){
                builder.field("title", title);
            }
            if(source != null && !source.isEmpty()){
                builder.field("source", source);
            }
            if(editor != null && !editor.isEmpty()){
                builder.field("editor", editor);
            }
            if(publishDate != null){
                builder.field("publish_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(publishDate));
            }
            if(desc != null && !desc.isEmpty()){
                builder.field("desc", desc);
            }
//            if(fileName != null && fileName > 0){
//                builder.field("file_name", fileName);
//            }
//            if(wordCount != null && wordCount > 0){
//                builder.field("word_count", wordCount);
//            }
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
     * 文档高级查询
     * @param gtWordCount
     * @param ltWordCount
     * @param gtPublishDate
     * @param ltPublishDate
     * @param pageNum
     * @return
     */
    @GetMapping("/query/document")
    public ResponseEntity query(
            @RequestParam(name = "gt_word_count",defaultValue = "0")Integer gtWordCount,
            @RequestParam(name = "lt_word_count",required = false)Integer ltWordCount,
            @RequestParam(name = "gt_publish_date",required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd") Date gtPublishDate,
            @RequestParam(name = "lt_publish_date",required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd")Date ltPublishDate,
            @RequestParam(name = "page_num",defaultValue = "1")Integer pageNum) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
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

        SearchRequest searchRequest = new SearchRequest(NEWS_DOCUMENT_INDEX).source(builder);
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
     * 文档高级查询
     * @param queryContent
     * @param pageNum
     * @return
     */
    @GetMapping("/query/global/document")
    public ResponseEntity query(
            @RequestParam(name = "query_content",defaultValue = "")String queryContent,
            @RequestParam(name = "page_num",defaultValue = "1")Integer pageNum) {

        String title = "title";
        String source = "source";
        String editor = "editor";
        String desc = "desc";
        String featureWords = "feature_words";

        MultiMatchQueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(queryContent,title,source,editor,desc,featureWords);
        SearchSourceBuilder builder = new SearchSourceBuilder().query(multiMatchQuery).from((pageNum-1)*DEFAULT_PAGE_SIZE).size(DEFAULT_PAGE_SIZE);
        SearchRequest searchRequest = new SearchRequest(NEWS_DOCUMENT_INDEX).source(builder);
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
