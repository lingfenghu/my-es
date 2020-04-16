package cn.hulingfeng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * @author hlf
 * @title: EsApplication
 * @projectName es
 * @description: TODO
 * @date 2020/2/6 21:46
 */
@SpringBootApplication
public class ESApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(ESApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(ESApplication.class);
    }
}
