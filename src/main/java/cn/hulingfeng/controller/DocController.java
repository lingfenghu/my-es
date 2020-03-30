package cn.hulingfeng.controller;

import cn.hulingfeng.service.DocService;
import cn.hulingfeng.service.ESService;
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
    private DocService docService;
    @Autowired
    private ESService esService;

    @PostMapping("file_upload")
    public ResponseEntity fileUpload(@RequestParam MultipartFile file) {
        if(file.isEmpty()){
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        return docService.upload(file);
    }

    @GetMapping("file_download")
    public ResponseEntity fileDownload(HttpServletResponse httpServletResponse, @RequestParam(name = "file_name")String fileName) throws FileNotFoundException {
        if(!docService.download(httpServletResponse,fileName)){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    @GetMapping("file_details")
    public ResponseEntity fileDetails(
            @RequestParam(name = "title") String title,
            @RequestParam(name = "publish_date")
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date publishDate,
            @RequestParam(name = "source") String source,
            @RequestParam(name = "editor") String editor,
            @RequestParam(name = "desc",required = false) String desc,
            @RequestParam(name = "file_name") String fileName,
            @RequestParam(name = "word_count") Integer wordCount,
            @RequestParam(name = "feature_words") String featureWords){

        return esService.add(title,source,editor,publishDate,desc,fileName,wordCount,featureWords);
    }
}
