package cn.hulingfeng.controller;

import cn.hulingfeng.utils.ExcelUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author hlf
 * @title: NginxLogController
 * @projectName es
 * @description: TODO
 * @date 2020/3/6 21:07
 */
@RestController
public class NginxLogController {

    @Autowired
    private RestHighLevelClient client;

    private ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger log = LoggerFactory.getLogger(NginxLogController.class);

    /**
     * 查询某一天的nginx日志
     * @param date
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("query/nginx_log")
    public ResponseEntity query(
            @RequestParam(name = "date",required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
            @RequestParam(name = "page_num",required = false) Integer pageNum,
            @RequestParam(name = "page_size",required = false) Integer pageSize
            ) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
        SearchSourceBuilder builder = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()).from((pageNum-1)*pageSize).size(pageSize);
        String indexName = "nginx-access-"+format.format(date);
        SearchRequest searchRequest = new SearchRequest(indexName).source(builder);
        //检查改天日志是否导入elasticsearch
        if(!isExistIndex(indexName)){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Map result = new HashMap();
        result.put("total",searchResponse.getHits().getTotalHits().value);
        List<Map<String,Object>> list = new ArrayList<>();
        for(SearchHit hit : searchResponse.getHits()){
            list.add(hit.getSourceAsMap());
        }
        result.put("list",list);
        try {
            return new ResponseEntity(objectMapper.writeValueAsString(result),HttpStatus.OK);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 判断索引是否存在
     * @param indexName
     * @return
     */
    public boolean isExistIndex(String indexName){
        GetIndexRequest request = new GetIndexRequest(indexName);
        boolean exists = false;
        try {
            exists = client.indices().exists(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return exists;
    }


    /**
     * 打印日志到Excel
     * @param date
     * @param pageNum
     * @param pageSize
     * @param response
     * @return
     */
    @GetMapping("nginx_logs_excel")
    public ResponseEntity getExcel(
            HttpServletResponse response,
            @RequestParam(name = "date",required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
            @RequestParam(name = "page_num",required = false) Integer pageNum,
            @RequestParam(name = "page_size",required = false) Integer pageSize){
        int rowNum = 0;
        Date now = new Date();
        response.reset();
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + now.getTime() +".xls");
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);

        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
        SearchSourceBuilder builder = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()).from((pageNum-1)*pageSize).size(pageSize);
        String indexName = "nginx-access-"+format.format(date);
        SearchRequest searchRequest = new SearchRequest(indexName).source(builder);
        if(!isExistIndex(indexName)){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        List<Map<String,Object>> list = new ArrayList<>();
        for(SearchHit hit : searchResponse.getHits()){
            list.add(hit.getSourceAsMap());
        }

        String[] headers = new String[]{"#","Request","RemoteAddr","TimeLocal","BytesSent","HttpUserAgent","RemoteUser",
                "Path", "HttpReferer","Host","Status"};
        //创建HSSFWorkbook对象
        HSSFWorkbook hssfWorkbook = new HSSFWorkbook();
        //创建HSSFSheet对象
        HSSFSheet sheet = hssfWorkbook.createSheet("sheet0");
        //创建HSSFRow对象
        HSSFRow rowHeader = sheet.createRow(rowNum++);
        HSSFCellStyle cellStyle = ExcelUtils.createCellStyle(hssfWorkbook,(short) 12,true, HorizontalAlignment.CENTER);
        Cell cell = null;
        int length = headers.length;
        for (int i = 0;i < length;i++){
            cell = rowHeader.createCell(i);
            cell.setCellStyle(cellStyle);
            cell.setCellValue(headers[i]);
            sheet.setColumnWidth(i, 256 * 18);
        }
        for(Map map : list){
            Row row = sheet.createRow(rowNum++);
            for(int i=0; i<length; i++){
                cell = row.createCell(i);
                switch(i){
                    case 0: cell.setCellValue(rowNum-1);break;
                    case 1: cell.setCellValue(map.get("request").toString());break;
                    case 2: cell.setCellValue(map.get("remote_addr").toString());break;
                    case 3: cell.setCellValue(map.get("time_local").toString());break;
                    case 4: cell.setCellValue(map.get("bytes_sent").toString());break;
                    case 5: cell.setCellValue(map.get("http_user_agent").toString());break;
                    case 6: cell.setCellValue(map.get("remote_user").toString());break;
                    case 7: cell.setCellValue(map.get("path").toString());break;
                    case 8: cell.setCellValue(map.get("http_referer").toString());break;
                    case 9: cell.setCellValue(map.get("host").toString());break;
                    case 10: cell.setCellValue(map.get("status").toString());break;
                    default:
                }
            }
        }
        try {
            OutputStream output = response.getOutputStream();
            hssfWorkbook.write(output);
            output.flush();
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ResponseEntity(HttpStatus.OK);
    }

}
