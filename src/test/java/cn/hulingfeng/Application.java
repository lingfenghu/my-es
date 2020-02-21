package cn.hulingfeng;

import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * es api测试
 * @author hlf
 * @title: AppTest
 * @projectName es
 * @description: TODO
 * @date 2020/2/6 21:48
 */
@SpringBootTest
public class Application {

    @Test
    public void getClient() throws IOException {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")));
        System.out.println(client.toString());
        client.close();
    }

    @Test
    public void indexApi() throws IOException {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")));
        //索引
        Map<String, Object> jsonMap = new HashMap<String, Object>();
        jsonMap.put("user", "kimchy");
        jsonMap.put("postDate", new Date());
        jsonMap.put("message", "trying out Elasticsearch");
        IndexRequest indexRequest = new IndexRequest("posts")
                .id("2").source(jsonMap);

        //执行
        IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);

        //分析返回信息
        String index = indexResponse.getIndex();
        String id = indexResponse.getId();
        if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {

        } else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {

        }
        ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
        if (shardInfo.getTotal() != shardInfo.getSuccessful()) {

        }
        if (shardInfo.getFailed() > 0) {
            for (ReplicationResponse.ShardInfo.Failure failure :
                    shardInfo.getFailures()) {
                String reason = failure.reason();
            }
        }
        System.out.println("index:"+index+"id"+id);
        //异步执行
//		client.indexAsync(request, RequestOptions.DEFAULT, new ActionListener<IndexResponse>() {
//			@Override
//			public void onResponse(IndexResponse indexResponse) {
//
//			}
//
//			@Override
//			public void onFailure(Exception e) {
//
//			}
//		});
        client.close();
    }

    @Test
    public void deleteIndexApi() throws IOException {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")));
        //索引删除请求
        DeleteRequest deleteRequest = new DeleteRequest("customer","1");
        //执行
        DeleteResponse deleteResponse =  client.delete(deleteRequest,RequestOptions.DEFAULT);
        client.close();
    }

    @Test
    public void getApi() throws IOException {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")));
        GetRequest getRequest = new GetRequest("posts", "1");
        String[] includes = new String[]{"message", "*Date"};
        String[] excludes = Strings.EMPTY_ARRAY;
        FetchSourceContext fetchSourceContext =
                new FetchSourceContext(true, includes, excludes);
        getRequest.fetchSourceContext(fetchSourceContext);
        //当请求的索引不存在时，需要处理异常
        try {
            GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
            String index = getResponse.getIndex();
            String id = getResponse.getId();
            if (getResponse.isExists()) {
                long version = getResponse.getVersion();
                String sourceAsString = getResponse.getSourceAsString();
                Map<String, Object> sourceAsMap = getResponse.getSourceAsMap();
                byte[] sourceAsBytes = getResponse.getSourceAsBytes();
            } else {

            }
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {

            }
        }
        client.close();
    }

    @Test
    public void updateApi() throws IOException {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http")));
        Map<String, Object> jsonMap = new HashMap<String, Object>();
        jsonMap.put("updated", new Date());
        jsonMap.put("reason", "daily update");
        jsonMap.put("user", "xiaoming");
        UpdateRequest request = new UpdateRequest("posts", "1").doc(jsonMap);
        try {
            UpdateResponse updateResponse = client.update(request, RequestOptions.DEFAULT);
            String index = updateResponse.getIndex();
            String id = updateResponse.getId();
            long version = updateResponse.getVersion();
            if (updateResponse.getResult() == DocWriteResponse.Result.CREATED) {

            } else if (updateResponse.getResult() == DocWriteResponse.Result.UPDATED) {

            } else if (updateResponse.getResult() == DocWriteResponse.Result.DELETED) {

            } else if (updateResponse.getResult() == DocWriteResponse.Result.NOOP) {

            }
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {

            }
        }
        client.close();
    }

    @Test
    public void bulkApi() throws IOException {

        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http")));
        //仅支持JSON和SMILE文档格式
        BulkRequest request = new BulkRequest();
        request.add(new DeleteRequest("posts", "1"));
        request.add(new UpdateRequest("posts", "2")
                .doc(XContentType.JSON,"other", "test"));
        request.add(new IndexRequest("posts").id("4")
                .source(XContentType.JSON,"field", "baz"));
        BulkResponse bulkResponse = client.bulk(request, RequestOptions.DEFAULT);

        for (BulkItemResponse bulkItemResponse : bulkResponse) {
            if (bulkItemResponse.isFailed()) {
                BulkItemResponse.Failure failure =
                        bulkItemResponse.getFailure();
            }
            DocWriteResponse itemResponse = bulkItemResponse.getResponse();

            switch (bulkItemResponse.getOpType()) {
                case INDEX:
                case CREATE:
                    IndexResponse indexResponse = (IndexResponse) itemResponse;break;
                case UPDATE:
                    UpdateResponse updateResponse = (UpdateResponse) itemResponse;break;
                case DELETE:
                    DeleteResponse deleteResponse = (DeleteResponse) itemResponse;
            }
        }
        client.close();
    }

    @Test
    public void searchApi() throws IOException {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http")));
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchRequest.source(searchSourceBuilder);
        MatchQueryBuilder matchQueryBuilder =
                new MatchQueryBuilder("user", "kimchy");
        searchSourceBuilder.query(matchQueryBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
    }
}
