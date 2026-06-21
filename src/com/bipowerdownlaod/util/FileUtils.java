package com.bipowerdownlaod.util;

import java.io.File;

public class FileUtils {
    //获取本地文件大小
    public static  long getFileContentSize(String path){
        File file=new File(path);
        return  file.exists()&&file.isFile()?file.length():0;
    }



}
