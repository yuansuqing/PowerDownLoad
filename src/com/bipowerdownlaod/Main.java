package com.bipowerdownlaod;

import com.bipowerdownlaod.core.Downloader;
import com.bipowerdownlaod.util.LogUtils;

import java.time.LocalTime;
import java.util.Scanner;

//主类
public class Main {
    public static void main(String[] args) throws  Exception {
            String url=null;
        Thread.currentThread().setName("主下载线程");
        LocalTime time=LocalTime.now();
            if(args== null||args.length==0){
                while (true){
                    LogUtils.info("请输入下载地址：");
                    Scanner sc=new Scanner(System.in);
                    url=sc.next();
                    if(url!=null){
                        break;
                    }
                }
            }
            else {
                url=args[0];
            }
        Downloader.download(url);
        LocalTime time1=LocalTime.now();
        
        // ✅ 使用 Duration 计算耗时，避免跨天等问题
        java.time.Duration duration = java.time.Duration.between(time, time1);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        LogUtils.info("下载完成，耗时：{}时 {}分 {}秒", hours, minutes, seconds);
    }
}
