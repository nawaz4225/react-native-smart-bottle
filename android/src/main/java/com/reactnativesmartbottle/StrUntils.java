package com.reactnativesmartbottle;

import java.util.ArrayList;
import java.util.List;

/**
 * @class name：com.fmk.test.skintwo
 * @class describe
 * @anthor zhuhao
 * @time 2018-1-28 14:46
 */
public class StrUntils {

    public static List<ArrayList<String>> strToList(String vailStr){
        List baDate = new ArrayList<String>();
        int dateJiGe = 0;
        do {
            baDate.add(vailStr.substring(dateJiGe,dateJiGe + 8));
            dateJiGe += 8;
        }while (dateJiGe < vailStr.length());
        System.out.println("切割数据:" + baDate);
        List youDates = new ArrayList<String>();
        for (int i = 0; i < baDate.size(); i++) {
            String singleDate = (String) baDate.get(i);
            if (!(singleDate.endsWith("0001"))){
                youDates.add(singleDate);
            }
        }
        System.out.println("有效数据:" + youDates);
        List jxList = new ArrayList<ArrayList<String>>();
        for (int i = 0; i < youDates.size(); i++) {
            List singleList = new ArrayList<String>();
            String itemStr = (String) youDates.get(i);
            int itemJianGe = 0;
            do {
                singleList.add(itemStr.substring(itemJianGe,itemJianGe + 2));
                itemJianGe += 2;
            }while (itemJianGe < itemStr.length());
            jxList.add(singleList);
        }
        System.out.println("item数据:" + jxList);
        return jxList;
    }

    /**
     * 数组转换成十六进制字符串
     * @param bArray
     * @return sb.toString()
     */
    public static String bytesToHexString(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 16进制转10进制
     */
    public static int hexToTen(String hex) {
        if (null == hex || (null != hex && "".equals(hex))) {
            return 0;
        }
        return Integer.valueOf(hex, 16);
    }


    /**
     * 多个字节的校验和
     * @param datas
     * @return
     */
    public static byte getXor(byte[] datas) {
        byte temp = datas[0];
        for (int i = 1; i < datas.length; i++) {
            temp ^= datas[i];
        }
        return temp;
    }

    /**
     * 把十六进制字符串转成字符数组
     * @param value
     * @return
     */
    public static List<String> stringToStrZu(String value) {
        List<String> deviceValue = new ArrayList<>();
        int count = value.length();
        int index = 0;
        int end = 2;
        while (count > 0) {
            deviceValue.add(value.substring(index, end));
            index += 2;
            end += 2;
            count -= 2;
        }
        return deviceValue;
    }

    /**
     * 字节数组转化为标准的16进制字符串
     *
     * @param bytes 字节数组数据
     * @return 字符串
     */
    public static String bytesToString(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = hexArray[v >>> 4];
            hexChars[i * 2 + 1] = hexArray[v & 0x0F];

            sb.append(hexChars[i * 2]);
            sb.append(hexChars[i * 2 + 1]);
            sb.append(' ');
        }
        return sb.toString();
    }


    /**
     * CRC8 校验 多项式  x8+x2+x+1
     * @param data
     * @return  校验和
     */
    public static  byte calcCrc8(byte[] data){
        byte crc = 0;
        for (int j = 0; j < data.length; j++) {
            crc ^= data[j];
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x80) != 0) {
                    crc = (byte) ((crc)<< 1);
                    crc ^= 0x107;
                } else {
                    crc = (byte) ((crc)<< 1);
                }
            }
        }
        return crc;
    }

}
