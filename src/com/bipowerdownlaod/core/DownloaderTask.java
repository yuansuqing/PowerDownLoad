package com.bipowerdownlaod.core;

import com.bipowerdownlaod.Constant.constant;
import com.bipowerdownlaod.util.HttpUtils;
import com.bipowerdownlaod.util.LogUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.util.concurrent.Callable;

//下载线程
public class DownloaderTask implements Callable<Boolean> {

    private  String url;
    // 下载起始位置
    private  Long start;
    // 下载结束位置
    private  Long end;

    // 表示当前分块的下载位置
    private  int part;
    
    // 添加进度追踪引用
    private DownloadInfoThread downloadInfoThread;

    public  DownloaderTask(String url, Long start, Long end, int part, DownloadInfoThread downloadInfoThread) {
        this.url = url;
        this.start = start;
        this.end = end;
        this.part = part;
        this.downloadInfoThread = downloadInfoThread;
    }

    @Override
    public Boolean call() throws Exception {
        //获取文件名
        String httFilename = HttpUtils.getHttpsFileName(url);
        //所有线程使用同一个文件
        httFilename = constant.PATH + File.separator + httFilename;

        //获取分块的下载连接
        HttpsURLConnection httpsURLConnection = HttpUtils.getHttpsURLConnection(url, start, end);

        try(BufferedInputStream bis = new BufferedInputStream(httpsURLConnection.getInputStream());
            RandomAccessFile accessFile = new RandomAccessFile(httFilename, "rw")){
            
            accessFile.seek(start);
            byte[] bytes = new byte[constant.BYTE_SIZE];
            int len;
            long writtenBytes = 0;
            
            while ((len = bis.read(bytes)) != -1){
                accessFile.write(bytes, 0, len);
                writtenBytes += len;
                
                if(downloadInfoThread != null){
                    downloadInfoThread.downSize.add(len);
                }
            }
            
            LogUtils.info("线程 {} 下载完成，写入 {} bytes ({} - {})", 
                         part, writtenBytes, start, end == -1 ? "EOF" : end);

        }catch (FileNotFoundException e){
            LogUtils.error("文件不存在: {}", url);
            return false;
        }catch (Exception e){
            LogUtils.error("线程 {} 下载失败: {}, 区间: {} - {}", 
                          part, e.getMessage(), start, end == -1 ? "EOF" : end);
            e.printStackTrace();
            return false;
        }finally {
            // ✅ 确保关闭连接
            if(httpsURLConnection != null){
                httpsURLConnection.disconnect();
            }
        }

        return true;
    }
}
