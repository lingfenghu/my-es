package cn.hulingfeng.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author hlf
 * @title: NginxLogController
 * @projectName es
 * @description: TODO
 * @date 2020/3/6 21:07
 */
@RestController
public class NginxLogController {

    @Autowired
    private RestHighLevelClient client;

    private ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("query/nginx_log")
    public ResponseEntity query(
            @RequestParam(name = "date",required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
            @RequestParam(name = "page_num",required = false) Integer pageNum,
            @RequestParam(name = "page_size",required = false) Integer pageSize
            ) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
        SearchSourceBuilder builder = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()).from(pageNum-1).size(pageSize);
        String indexName = "nginx-access-"+format.format(date);
        SearchRequest searchRequest = new SearchRequest(indexName).source(builder);

        if(!isExistIndex(indexName)){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

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
     * 判断索引是否存在
     * @param indexName
     * @return
     */
    public boolean isExistIndex(String indexName){
        GetIndexRequest request = new GetIndexRequest(indexName);
        boolean exists = false;
        try {
            exists = client.indices().exists(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return exists;
    }
}
