package cn.hulingfeng.controller;

import cn.hulingfeng.service.DocService;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author hlf
 * @title: DocController
 * @projectName es
 * @description: TODO
 * @date 2020/2/8 14:42
 */
@RestController
public class DocController {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private DocService docService;

    @PostMapping("file_upload")
    public ResponseEntity fileUpload(@RequestParam MultipartFile file) throws IOException {
        if(file.isEmpty()){
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        return docService.upload(file);
    }

    @GetMapping("file_download")
    public void fileDownload(HttpServletResponse httpServletResponse, @RequestParam(name = "file_name")String fileName) throws FileNotFoundException {
        docService.download(httpServletResponse,fileName);
    }

    @GetMapping("file_details")
    public org.springframework.http.ResponseEntity fileDetails(
            @RequestParam(name = "title") String title,
            @RequestParam(name = "publish_date")
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date publishDate,
            @RequestParam(name = "source") String source,
            @RequestParam(name = "editor") String editor,
            @RequestParam(name = "desc",required = false) String desc,
            @RequestParam(name = "file_name") String fileName,
            @RequestParam(name = "word_count") String wordCount){
        try {
            XContentBuilder xContent = XContentFactory.jsonBuilder()
                    .startObject()
                    .field("title", title)
                    .field("publish_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(publishDate))
                    .field("source", source)
                    .field("editor", editor)
                    .field("desc", desc)
                    .field("file_name", fileName)
                    .field("word_count", wordCount)
                    .endObject();
            IndexRequest request = new IndexRequest("news002").source(xContent);
            IndexResponse result = this.client.index(request, RequestOptions.DEFAULT);
            return new org.springframework.http.ResponseEntity(result.getId(), HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new org.springframework.http.ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
