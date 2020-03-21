package cn.hulingfeng;

import cn.hulingfeng.utils.FileUtils;
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

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public void getWordCount() throws IOException {
        int count = 0;
        String regex = "[\\u4E00-\\u9FA5|，|。|；|“|”|：|、|！|？|......|{|}|（|）|《|》|\\d]";
        File file = new File("C:\\Users\\HLF\\Desktop\\bs\\news_doc\\钟南山：今天有个好消息.txt");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String lineStr;
        while (( lineStr = bufferedReader.readLine())!=null){
            count = 0;
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(lineStr);
            while(matcher.find()){
                count++;
            }

            System.out.println(lineStr+"|"+lineStr.length()+"|"+count);
        }
    }

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

    @Test
    public void fetch163TopNewsUrl() throws IOException {
//        Document document = Jsoup.connect("https://news.163.com/").validateTLSCertificates(false).get();
//        Element topNews = document.getElementById("js_top_news");
//        Elements elements = topNews.select("a");
//        for(Element a : elements){
//            System.out.println(a.attr("href"));
//            fetch163News(a.attr("href"));
//        }
//        https://news.163.com/20/0317/09/F7TNS8QQ000189FH.html
//        https://news.163.com/20/0317/11/F7TRQ66M000189FH.html
//        https://news.163.com/20/0317/11/F7TRSNRK000189FH.html
//        https://news.163.com/20/0317/09/F7TMFT0I0001899O.html
//        https://tech.163.com/20/0317/07/F7TGOSHO00097U7R.html
//        https://news.163.com/20/0317/02/F7SSJNOE0001899O.html
//        https://news.163.com/20/0317/07/F7TFS0K00001899O.html
//        https://news.163.com/20/0317/00/F7SN46G10001899O.html
//        https://news.163.com/20/0317/02/F7SVNT7U0001899O.html
//        https://news.163.com/20/0316/23/F7SJDN0H0001899O.html
//        https://news.163.com/20/0317/01/F7SQ4A4K0001899O.html
//        https://news.163.com/20/0317/04/F7T65G2U00019B3E.html
//        https://news.163.com/20/0317/04/F7T6DFIM0001899O.html
    }

    @Test
    public void fetch163News(String url) throws IOException {
        //https://news.163.com/20/0317/02/F7SSJNOE0001899O.html  新闻样例
        //https://blog.csdn.net/u014256984/article/details/73330573 证书问题解决
//        Document document = Jsoup.connect(url).validateTLSCertificates(false).get();
//
////        Document document = Jsoup.connect("https://news.163.com/20/0317/11/F7TRSNRK000189FH.html").validateTLSCertificates(false).get();
//        Element postContentMain = document.getElementsByClass("post_content_main").first();
//        Element h1 = postContentMain.getElementsByTag("h1").first();
//        Element postTimeSource = document.getElementsByClass("post_time_source").first();
//        Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
//        Matcher matcher = pattern.matcher(postTimeSource.text());
//        String dateTime = "";
//        if(matcher.find()){
//            dateTime = matcher.group();
//        }
//        Element a = postTimeSource.getElementsByTag("a").first();
//
//        String article = "";
//        Element postText = document.getElementsByClass("post_text").first();
//        Elements elements = postText.getElementsByTag("p");
//        for(Element p : elements){
//            article += p.text();
////            System.out.println(p.text());
//        }
////        Element p = postContentMain.getElementsByTag("p").get(3);
////        System.out.println(p);
//
//        Element epSource = postText.getElementsByClass("ep-source cDGray").first();
//        Element span = epSource.getElementsByTag("span").get(1);
////                postText.select(".ep-source").select(".cDGray").first();
//        String[] epEditor = span.text().split("\\：");
////        System.out.println(epEditor[1]);
//
//        String title = h1.text();
////        Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateTime);
//        String source = a.text();
//        String editor = epEditor[1];
//        String content = article;
//        String desc = elements.get(1).text();
//
////        System.out.println("title:"+title+"\ndate:"+dateTime+"\nsource:"+source+"\neditor:"+editor+"\ncontent:"+content);
//        String news = title+"||"+dateTime+"||"+source+"||"+editor+"||"+desc;
//
//        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(news.getBytes());
//        String fileName = System.currentTimeMillis()+".txt";
//        File file = new File(FileUtils.PATH+fileName);
//        FileOutputStream fileOutputStream = new FileOutputStream(file);
//        fileOutputStream.write(news.getBytes());
//        fileOutputStream.close();
    }


}
