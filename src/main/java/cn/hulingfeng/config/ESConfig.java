package cn.hulingfeng.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ElasticSearch配置类
 * @author hlf
 * @title: ESConfig
 * @projectName es
 * @description: TODO
 * @date 2020/2/18 20:21
 */
@Configuration
public class ESConfig {

    /**
     * 客户端client配置
     * @return
     */
    @Bean
    public RestHighLevelClient client(){
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http")));
        return client;
    }
}
