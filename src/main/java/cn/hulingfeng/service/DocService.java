package cn.hulingfeng.service;

import cn.hulingfeng.utils.FileUtils;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.AnalyzeRequest;
import org.elasticsearch.client.indices.AnalyzeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Logger log = LoggerFactory.getLogger(DocService.class);

    /**
     * 文件上传服务
     * @param file
     * @return
     */
    public ResponseEntity upload(MultipartFile file) {
        int wordCount;
        String feature_words;
        //文件名和文件类型
        String originalFilename = file.getOriginalFilename();
        String fileName = System.currentTimeMillis()+originalFilename.substring(originalFilename.lastIndexOf("."));
        //将路径转换为绝对路径
        File dest = null;
        try {
            dest = ResourceUtils.getFile(FileUtils.UPLOAD_PATH+fileName).getAbsoluteFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //父目录不存在创建父目录
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdir();
        }
        //检验该文件是否已经存在
        if(dest.exists()){
            return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
        }
        try {
            file.transferTo(dest);
            wordCount = countWord(fileName);
            feature_words = filterKeywords(fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        //上传文件其他信息
        Map extra = new HashMap();
        extra.put("word_count",wordCount);
        extra.put("file_name",fileName);
        extra.put("feature_words",feature_words);
        return new ResponseEntity(extra,HttpStatus.OK);
    }

    /**
     * 文件下载服务
     * @param httpServletResponse
     * @param fileName
     */
    public boolean download(HttpServletResponse httpServletResponse, String fileName) throws FileNotFoundException {
        //设置编码格式和打开文件方式
        httpServletResponse.setCharacterEncoding("utf-8");
        httpServletResponse.setContentType("application/octet-stream");
//        System.out.println(ResourceUtils.getFile(FileUtils.DOWNLOAD_PATH+fileName).getAbsoluteFile());
        File file = ResourceUtils.getFile(FileUtils.UPLOAD_PATH+fileName);
        if (!file.exists()) {
            return false;
        }
        try {
            //由于get请求会显示在url上,中文会被转译成urlcode,下载成文件需要解码
            httpServletResponse.setHeader("Content-Disposition", "attachment;fileName="+ URLEncoder.encode(fileName, "utf-8"));
            InputStream inputStream =
//                    new ClassPathResource(fileName).getInputStream();
                    new FileInputStream(file);
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
        File file = ResourceUtils.getFile(FileUtils.UPLOAD_PATH+fileName);
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
                //设置特征词数量限制
                if(featureWords.size() >= 50) {
                    break;
                }
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
        //媒体，热词，城市，人物词典
        InputStream media = getClass().getResourceAsStream("/static/media.dic");
        InputStream hotword = getClass().getResourceAsStream("/static/hotword.dic");
        InputStream city = getClass().getResourceAsStream("/static/city.dic");
        InputStream figure = getClass().getResourceAsStream("/static/figure.dic");

        BufferedReader mediaDicReader = new BufferedReader(new InputStreamReader(media));
        BufferedReader hotWordDicReader = new BufferedReader(new InputStreamReader(hotword));
        BufferedReader cityDicReader = new BufferedReader(new InputStreamReader(city));
        BufferedReader figureDicReader = new BufferedReader(new InputStreamReader(figure));

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

    /**
     * 统计字数
     * @param fileName
     * @return
     * @throws IOException
     */
    public static Integer countWord(String fileName) throws IOException {
        int wordNum = 0;
        //统计字数包含中文，中文常用标点符号，字母，数字以及下划线
        String regex = "[\\u4E00-\\u9FA5|，|。|；|“|”|：|、|！|？|......|{|}|（|）|《|》|\\w]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher;
        File file = ResourceUtils.getFile(FileUtils.UPLOAD_PATH+fileName);
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
}
