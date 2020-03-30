package cn.hulingfeng.service;

import cn.hulingfeng.controller.DocController;
import cn.hulingfeng.utils.FileUtils;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.AnalyzeRequest;
import org.elasticsearch.client.indices.AnalyzeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;

/**
 * @author hlf
 * @title: DocService
 * @projectName es
 * @description: TODO
 * @date 2020/2/8 14:47
 */
@Service
public class DocService {

    @Autowired
    private RestHighLevelClient client;

    private static final Logger log = LoggerFactory.getLogger(DocController.class);

    /**
     * 文件上传服务
     * @param file
     * @return
     */
    public ResponseEntity upload(MultipartFile file) {
        int wordCount = 0;
        String feature_words;
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
            feature_words = filterKeywords(fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        //上传文件其他信息
        Map others = new HashMap();
        others.put("word_count",wordCount);
        others.put("file_name",fileName);
        others.put("feature_words",feature_words);
        return new ResponseEntity(others,HttpStatus.OK);
    }

    /**
     * 文件下载服务
     * @param httpServletResponse
     * @param fileName
     */
    public boolean download(HttpServletResponse httpServletResponse, String fileName) {
        //设置编码格式和打开文件方式
        httpServletResponse.setCharacterEncoding("utf-8");
        httpServletResponse.setContentType("application/octet-stream");
        File file = new File(FileUtils.PATH+fileName);
        if (!file.exists()) {
            return false;
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
            return false;
        }
        return true;
    }

    /**
     * 文件关键词提取
     * @throws IOException
     */
    public String filterKeywords(String fileName) throws IOException {
        Set<String> featureWords = new HashSet<>();
        Map<String, Integer> frequencies = new HashMap<>();
        File file = new File(FileUtils.PATH+fileName);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = bufferedReader.readLine())!=null){
            AnalyzeRequest request = AnalyzeRequest.withGlobalAnalyzer("ik_smart", line);
            AnalyzeResponse response = client.indices().analyze(request, RequestOptions.DEFAULT);
            List<AnalyzeResponse.AnalyzeToken> tokens = response.getTokens();
            //分词结果
            for(AnalyzeResponse.AnalyzeToken term: tokens){
                if(isHitDictionary(term.getTerm())){
                    featureWords.add(term.getTerm());
                }
                wordsFrequencies(frequencies,term.getTerm());
            }
        }
        List<Map.Entry<String, Integer>> result = new ArrayList<>(frequencies.entrySet());
        Collections.sort(result, (o1, o2) -> o2.getValue() - o1.getValue());
        for(Map.Entry entry : result){
            if((Integer)entry.getValue() >= 3){
                featureWords.add((String) entry.getKey());
            }
        }
        return StringUtils.arrayToDelimitedString(featureWords.toArray()," ");
    }

    /**
     * 词频统计
     * @param frequencies
     * @param word
     * @return
     */
    public static Map<String,Integer> wordsFrequencies(Map<String, Integer> frequencies,String word){
        if(frequencies == null){
            frequencies = new HashMap<>();
        }
        if(word.isEmpty()||word == null){
            return frequencies;
        }
        if(frequencies.containsKey(word)){
            frequencies.put(word,frequencies.get(word)+1);
        }else {
            frequencies.put(word,1);
        }
        return frequencies;
    }

    /**
     * 分词字典命中判断
     * @param word
     * @return
     * @throws IOException
     */
    public boolean isHitDictionary(String word) throws IOException {
        //媒体，自定义热词，城市，人物词典
        File mediaDic = new File("src/main/resources/static/media.dic");
        File hotWordDic = new File("src/main/resources/static/hotword.dic");
        File cityDic = new File("src/main/resources/static/city.dic");
        File figureDic = new File("src/main/resources/static/figure.dic");

        BufferedReader mediaDicReader = new BufferedReader(new FileReader(mediaDic));
        BufferedReader hotWordDicReader = new BufferedReader(new FileReader(hotWordDic));
        BufferedReader cityDicReader = new BufferedReader(new FileReader(cityDic));
        BufferedReader figureDicReader = new BufferedReader(new FileReader(figureDic));

        String line;
        while ((line = hotWordDicReader.readLine())!=null){
            if(word.equals(line.trim())){
                return true;
            }
        }
        while ((line = mediaDicReader.readLine())!=null){
            if(word.equals(line.trim())){
                return true;
            }
        }
        while ((line = figureDicReader.readLine())!=null){
            if(word.equals(line.trim())){
                return true;
            }
        }
        while ((line = cityDicReader.readLine())!=null){
            if(word.equals(line.trim())){
                return true;
            }
        }
        return false;
    }
}
