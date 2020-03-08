package cn.hulingfeng.controller;

import cn.hulingfeng.service.DocService;
import cn.hulingfeng.utils.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;

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

    @PostMapping("file_upload")
    public ResponseEntity fileUpload(@RequestParam MultipartFile file){
        return docService.upload(file);
    }

    @GetMapping("file_download")
    public void fileDownload(HttpServletResponse httpServletResponse, @RequestParam(name = "file_name")String fileName) throws FileNotFoundException {
        docService.download(httpServletResponse,fileName);
    }
}
