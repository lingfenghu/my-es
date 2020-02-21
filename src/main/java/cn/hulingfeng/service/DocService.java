package cn.hulingfeng.service;

import cn.hulingfeng.controller.DocController;
import cn.hulingfeng.utils.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    //文档存储固定路径
    private static final String PATH = "src/main/resources/doc/";

    public ResponseEntity upload(MultipartFile file){
        if(file.isEmpty()){
            return ResponseEntity.getInstance("上传文件为空",501);
        }
        //基础路径
        String originalFilename = file.getOriginalFilename();
        log.info("上传了"+originalFilename);
        //重新命名文件
        String[] arr = originalFilename.split("\\.");
        String fileName = arr[0]+"_"+new SimpleDateFormat("yyMMddHHmmss").format(new Date())+"."+arr[1];
        //将路径转换为绝对路径
        File dest = new File(PATH+fileName).getAbsoluteFile();
        //检验该文件是否已经存在
        if(dest.exists()){
            return ResponseEntity.getInstance("上传文件已存在",501);
        }
        //父目录不存在创建父目录
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdir();
        }
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.getInstance("服务器错误",500);
        }
        return ResponseEntity.getInstance("上传文件成功",200,dest);
    }

    //返回对象只能是String否则报错
    public void download(HttpServletResponse httpServletResponse, String fileName) throws FileNotFoundException {
        //设置编码格式和打开文件方式
        httpServletResponse.setCharacterEncoding("utf-8");
        httpServletResponse.setContentType("application/octet-stream");
        File file = new File(PATH+fileName);
        if (!file.exists()) {
            throw new FileNotFoundException();
        }
        try {
            //由于get请求会显示在url上,中文会被转译成urlcode,下载成文件需要解码
            httpServletResponse.setHeader("Content-Disposition", "attachment;fileName="+ URLEncoder.encode(fileName, "utf-8"));
            InputStream inputStream = new FileInputStream(PATH+fileName);
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
