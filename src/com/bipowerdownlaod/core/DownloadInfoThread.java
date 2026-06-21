package com.bipowerdownlaod.core;

import com.bipowerdownlaod.Constant.constant;

import java.util.concurrent.atomic.LongAdder;

// 展示下载信息线程
public class DownloadInfoThread implements  Runnable{
    // 文件总大小（字节），从服务器获取后固定不变
    private final long httpFlieContentLength;


    // 构造方法：初始化文件总大小
    public DownloadInfoThread(long httpFlieContentLength, long downloadedSize){
        this.httpFlieContentLength = httpFlieContentLength;

    }
    
    // 断点续传：记录之前已经下载的部分
    public static LongAdder finishSize = new LongAdder();
    
    // volatile关键字：保证多线程可见性
    public static volatile LongAdder downSize = new LongAdder();
    
    // 上次下载的大小（字节），用于计算下载速度
    public volatile double preSize;

    @Override
    public void run() {
        // 1. 计算文件总大小（转换为 MB）
        String httpFileSize = String.format("%.2f", httpFlieContentLength / constant.MB) + "MB";

        // 2. 计算这一秒的下载速度（KB/s）
        int speed = (int)((downSize.doubleValue() - preSize) / 1024d);
        
        // 更新 preSize 为当前 downSize
        preSize = downSize.doubleValue();

        // 3. 计算剩余文件大小（字节）
        double remainSize = httpFlieContentLength - downSize.doubleValue() - finishSize.doubleValue();

        // 4. 估算剩余时间（秒）
        String remainTime;
        if (speed <= 0) {
            remainTime = "--";
        } else {
            remainTime = String.format("%.1f", remainSize / 1024d / speed);
        }
        
        // 5. 计算已下载大小（转换为 MB）
        double totalDownloaded = downSize.doubleValue() + finishSize.doubleValue();
        String currentFileSize = String.format("%.2f", totalDownloaded / constant.MB) + "MB";

        // 6. 计算下载百分比
        double progress = totalDownloaded / httpFlieContentLength * 100;
        String progressStr = String.format("%.2f%%", progress);

        // 7. 输出下载进度信息
        System.out.println("已下载：" + currentFileSize + "/" + httpFileSize +
                " (" + progressStr + ") " + speed + "kb/s   剩余：" + remainTime + "s");
    }
}
