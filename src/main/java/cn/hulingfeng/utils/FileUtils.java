package cn.hulingfeng.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件工具类
 * @author hlf
 * @title: FileUtils
 * @projectName es
 * @description: TODO
 * @date 2020/3/16 15:35
 */
public class FileUtils {

    /**
     * 文件存储固定路径,文件系统真实路径
     */
    public static final String PATH = "src/main/resources/doc/";

    /**
     * 统计字数
     * @param file
     * @return
     * @throws IOException
     */
    public static Integer countWord(File file) throws IOException {
        int wordNum = 0;
        //统计字数包含中文，中文常用标点符号，字母，数字以及下划线
        String regex = "[\\u4E00-\\u9FA5|，|。|；|“|”|：|、|！|？|......|{|}|（|）|《|》|\\w]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String lineStr;
        while (( lineStr = bufferedReader.readLine())!=null){
            matcher = pattern.matcher(lineStr);
            while(matcher.find()){
                wordNum++;
            }
        }
        return wordNum;
    }

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
