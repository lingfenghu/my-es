package cn.hulingfeng.service;

import cn.hulingfeng.controller.DocController;
import cn.hulingfeng.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hlf
 * @title: DocService
 * @projectName es
 * @description: TODO
 * @date 2020/2/8 14:47
 */
@Service
public class DocService {

    private static final Logger log = LoggerFactory.getLogger(DocController.class);

    /**
     * 文件上传服务
     * @param file
     * @return
     */
    public ResponseEntity upload(MultipartFile file) {
        int wordCount = 0;
        //文件名和文件类型
        String originalFilename = file.getOriginalFilename();
        String[] arr = originalFilename.split("\\.");
        //获取当前时间戳命名文件
        String fileName = System.currentTimeMillis()+"."+arr[1];
        String path = FileUtils.PATH+fileName;
        //将路径转换为绝对路径
        File dest = new File(path).getAbsoluteFile();
        //父目录不存在创建父目录
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdir();
        }
        //检验该文件是否已经存在
        if(dest.exists()){
            return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
        }
        try {
            wordCount = FileUtils.countWord(FileUtils.MultipartFileToFile(file));
            //文件信息必须在转换之前处理
            file.transferTo(dest);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        //上传文件其他信息
        Map others = new HashMap();
        others.put("word_count",wordCount);
        others.put("file_name",fileName);
        return new ResponseEntity(others,HttpStatus.OK);
    }

    /**
     * 文件下载服务
     * @param httpServletResponse
     * @param fileName
     */
    public void download(HttpServletResponse httpServletResponse, String fileName) {
        //设置编码格式和打开文件方式
        httpServletResponse.setCharacterEncoding("utf-8");
        httpServletResponse.setContentType("application/octet-stream");
        File file = new File(FileUtils.PATH+fileName);
        if (!file.exists()) {
            try {
                throw new FileNotFoundException();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        try {
            //由于get请求会显示在url上,中文会被转译成urlcode,下载成文件需要解码
            httpServletResponse.setHeader("Content-Disposition", "attachment;fileName="+ URLEncoder.encode(fileName, "utf-8"));
            InputStream inputStream = new FileInputStream(FileUtils.PATH+fileName);
            OutputStream outputStream = httpServletResponse.getOutputStream();
            //创建数据缓冲区，一个个字节读取
            byte[] b = new byte[1024];
            int length = 0;
            while ((length = inputStream.read(b))>0){
                outputStream.write(b,0,length);
            }
            outputStream.flush();
            inputStream.close();
            outputStream.close();
        }  catch (IOException e) {
            e.printStackTrace();
        }
    }

}
