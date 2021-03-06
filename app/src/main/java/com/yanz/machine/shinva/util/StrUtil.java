package com.yanz.machine.shinva.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by yanzi on 2016-04-28.
 */
public class StrUtil {
    //获取日期
    public static String getChineseTime(){
        Date date = new Date();
        String sDate = date.toString();
        System.out.println("*****"+sDate);
        return sDate;
    }
    //获取时间
    public static String getSimpleDateFormateDateTime(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd@HH:mm:ss");
        String data = dateFormat.format(new Date());
        return data;
    }
    public static String getYearAndMonth(){
        String yearAndMonth = getSimpleDateFormateDateTime().substring(0,7);
        return yearAndMonth;
    }
    //排序顺序
    public static final Integer ASC = 0;
    public static final Integer DESC = 1;

    public static String getMapValueAsString(Map map){
        StringBuilder sb = new StringBuilder();
        Iterator iterator = map.entrySet().iterator();
        while (iterator.hasNext()){
            Entry entry = (Entry)iterator.next();
            String value = entry.getValue().toString();
            sb.append(value);
            sb.append(",");
        }
        String temp = sb.toString();
        if (StrUtil.isNotEmpty(temp)){
            return temp.substring(0,temp.length()-1);
        }
        return null;
    }
    public static boolean isNotEmpty(String str) {
        if (str == null || str.equals("[]") || str.equals("null")) {
            return false;
        } else {
            return !"".equals(str.trim());
        }
    }
    //List转String
    public static String ListToString(List<?> list){
        StringBuffer sb = new StringBuffer();
        if (list!=null && list.size()>0){
            for (int i=0;i<list.size();i++){
                if (list.get(i)==null||list.get(i)==""){
                    continue;
                }
                //如果值是list类型则调用自己
                if (list.get(i) instanceof List) {
                    sb.append(ListToString((List<?>) list.get(i)));
                    sb.append("@");
                }  else {
                    sb.append(list.get(i));
                    sb.append("@");
                }
            }
        }
        return "L"+sb.toString();
    }

}
