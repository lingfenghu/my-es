package cn.hulingfeng.utils;

import org.springframework.web.multipart.MultipartFile;
import java.io.*;

/**
 * 文件工具类
 * @author hlf
 * @title: FileUtils
 * @projectName es
 * @description: TODO
 * @date 2020/3/16 15:35
 */
public class FileUtils {

    public static final String UPLOAD_PATH = "src/main/resources/doc/";

    public static final String DOWNLOAD_PATH = "classpath:doc/";

    /**
     * MultipartFile to file
     * @param multipartFile
     * @return
     * @throws IOException
     */
    public static File MultipartFileToFile(MultipartFile multipartFile) throws IOException {
        File file = new File(multipartFile.getOriginalFilename());
        InputStream inputStream = multipartFile.getInputStream();
        OutputStream outputStream = new FileOutputStream(file);
        int len = 0;
        byte[] buffer = new byte[8192];
        while ((len = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
        }
        outputStream.flush();
        inputStream.close();
        outputStream.close();
        return file;
    }
}
