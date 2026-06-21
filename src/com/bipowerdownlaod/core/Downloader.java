package com.bipowerdownlaod.core;

import com.bipowerdownlaod.Constant.constant;
import com.bipowerdownlaod.util.FileUtils;
import com.bipowerdownlaod.util.HttpUtils;
import com.bipowerdownlaod.util.LogUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.*;

public class Downloader {
    // 创建一个线程池, 用于定时执行任务，创一个展示下载信息线程
    public static ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(
            1,
            r -> new Thread(r, "下载信息线程"),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    //创建一个线程池对象，用于执行下载任务
   private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            constant.THREAD_NUM,//核心线程数
            constant.THREAD_NUM,//最大线程数
            0,//线程空闲时间
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public  static  void  download(String  url) throws  IOException{
        //获取文件名
        String httpsFileName= HttpUtils.getHttpsFileName(url);

        //拼接成下载文件路径
        httpsFileName = constant.PATH+ File.separator +httpsFileName;
        File file = new File(httpsFileName);
        
        //获取本地文件大小
        long localFileLength = FileUtils.getFileContentSize(httpsFileName);

        // ✅ 重置静态计数器，避免多次下载累积
        DownloadInfoThread.finishSize.reset();
        DownloadInfoThread.downSize.reset();

        //获取链接对象
        HttpsURLConnection httpsURLConnection= null ;
        DownloadInfoThread downloadInfoThread = null;
        
        // ✅ 提升到方法级别，以便在 finally 块中访问
        long contentLength = 0;

       try{
           httpsURLConnection= HttpUtils.getHttpsURLConnection(url);
           httpsURLConnection.setConnectTimeout(10000);
           httpsURLConnection.setReadTimeout(30000);
           
           // 获取文件总大小
          contentLength = httpsURLConnection.getContentLength();
           
           // ✅ 判断文件是否已完整下载
            if(localFileLength >= contentLength && localFileLength > 0){
                LogUtils.info("{} 已下载完毕（{}/{} bytes），无需再次下载", 
                             httpsFileName, localFileLength, contentLength);
                return;
            } else if (localFileLength > 0) {
                LogUtils.error("发现未完成文件 {}（{}/{} bytes），继续下载...",
                             httpsFileName, localFileLength, contentLength);
            } else {
                System.out.println("开始下载...");
                System.out.println("URL: " + url);
                System.out.println("文件名: " + httpsFileName);
                System.out.println("保存路径: " + constant.PATH);
            }

            // ✅ 创建下载信息线程对象，传入已下载大小

            downloadInfoThread = new DownloadInfoThread(contentLength, localFileLength);
            
            // ✅ 设置断点续传的已完成大小
            //LongAdder 对象的方法，用于累加值
            DownloadInfoThread.finishSize.add(localFileLength);
            
            // 提交任务, 每隔1秒执行一次
           scheduledExecutorService.scheduleAtFixedRate(downloadInfoThread, 1, 1, TimeUnit.SECONDS);




           ArrayList<Future<Boolean>> futureList = new ArrayList<>();
           //分块对象 - 传入已下载大小
           //futureList 存储每个分块的Future对象任务
           split(url, futureList, downloadInfoThread, contentLength, localFileLength);

           futureList.forEach(future -> {
               try{
                   future.get();
               }catch (Exception e){
                   e.printStackTrace();
                   LogUtils.error("下载失败: {}", e.getMessage());
               }
           });


       } catch (Exception e){
           e.printStackTrace();
           LogUtils.error("获取连接失败: {}", e.getMessage());
           return;
       }
      finally {
           System.out.println("下载完成");
           
           // ✅ 验证下载的文件大小是否正确
           long finalFileSize = FileUtils.getFileContentSize(httpsFileName);
           if (downloadInfoThread != null && contentLength > 0) {
               double totalDownloaded = downloadInfoThread.downSize.doubleValue() + 
                                       DownloadInfoThread.finishSize.doubleValue();
               
               // ✅ 使用之前获取的 contentLength，不要重新请求
               LogUtils.info("已下载：{}MB (期望本次下载: {}MB, 总文件: {}MB)", 
                           String.format("%.2f", totalDownloaded / constant.MB),
                           String.format("%.2f", (contentLength - localFileLength) / constant.MB),
                           String.format("%.2f", contentLength / constant.MB));
               
               // ✅ 检查文件大小是否匹配（与原始文件总大小比较）
               if (Math.abs(finalFileSize - contentLength) > 1024) {  // 允许 1KB 误差
                   LogUtils.error("⚠️ 文件大小不匹配！实际: {} bytes, 期望: {} bytes", 
                                finalFileSize, contentLength);
                   LogUtils.error("差异: {} bytes ({} MB)", 
                                Math.abs(finalFileSize - contentLength),
                                Math.abs(finalFileSize - contentLength) / constant.MB);
                   LogUtils.error("文件可能损坏，请删除后重新下载！");
                   
                   // ✅ 提供删除文件的选项
                   LogUtils.error("建议执行: del \"{}\"", httpsFileName);
               } else {
                   LogUtils.info("✅ 文件下载成功，大小验证通过！");
               }
           }

           // 关闭链接对象
           if(httpsURLConnection!= null){
               httpsURLConnection.disconnect();
           }

           // 关闭线程池
           scheduledExecutorService.shutdown();
           threadPoolExecutor.shutdown();
          try{
              if(!scheduledExecutorService.awaitTermination(1000, TimeUnit.MILLISECONDS)){
                  scheduledExecutorService.shutdownNow();
              }
              if(!threadPoolExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS)){
                  threadPoolExecutor.shutdownNow();
              }
          }catch (InterruptedException e){
              e.printStackTrace();
              LogUtils.error("下载线程池关闭失败: {}", e.getMessage());
          }
       }


    }
    // 文件分块
    public  static void  split(String url, ArrayList<Future<Boolean>> futureList, 
                              DownloadInfoThread downloadInfoThread, 
                              long contentLength, long downloadedSize) throws Exception{
        try{
            // ✅ 计算剩余需要下载的大小
            long remainingSize = contentLength - downloadedSize;
            
            // 计算分块大小
            long blockSize = remainingSize / constant.THREAD_NUM;
            
            //计算分块个数
            for(int i=0;i < constant.THREAD_NUM;i++){
               // ✅ 从已下载的位置开始
               long startPos = downloadedSize + i * blockSize;
               long endPos;
               
               if(i == constant.THREAD_NUM - 1){
                   endPos = -1; // 最后一个分块到文件末尾
               }else {
                   endPos = downloadedSize + (i + 1) * blockSize - 1;
               }
               
                LogUtils.info("分块 {}：{} - {}", i, startPos, endPos == -1 ? "文件末尾" : endPos);
                
                DownloaderTask downloaderTask = new DownloaderTask(url, startPos, endPos, i, downloadInfoThread);

                // 提交任务到线程池中执行
                Future<Boolean> future = threadPoolExecutor.submit(downloaderTask);
                futureList.add(future);
            }
       }catch ( Exception e){
            LogUtils.error("分块文件失败: {}", e.getMessage());
        }
    }
}
