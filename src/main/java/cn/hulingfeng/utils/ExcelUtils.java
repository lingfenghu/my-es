package cn.hulingfeng.utils;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;

/**
 * Excel工具类
 * @author hlf
 * @title: Excel
 * @projectName es
 * @description: TODO
 * @date 2020/3/10 10:22
 */
public class ExcelUtils {

    /**
     * Excel小格样式配置
     * @param workbook
     * @param fontSize
     * @param isBold
     * @param horizontalAlignment
     * @return
     */
    public static HSSFCellStyle createCellStyle(HSSFWorkbook workbook, short fontSize, boolean isBold, HorizontalAlignment horizontalAlignment) {
        HSSFCellStyle style = workbook.createCellStyle();
        //设置水平位置
        style.setAlignment(horizontalAlignment);//水平居中
        style.setVerticalAlignment(VerticalAlignment.CENTER);//默认垂直居中
        //创建字体
        HSSFFont font = workbook.createFont();
        //是否加粗字体
        font.setBold(isBold);
        //设置字体大小
        font.setFontHeightInPoints(fontSize);
        //加载字体
        style.setFont(font);
        return style;
    }
}
