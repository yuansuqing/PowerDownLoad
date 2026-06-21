package com.bipowerdownlaod.util;
// 声明包名，表示这个类属于 com.bipowerdownlaod.util 包

import javax.net.ssl.HttpsURLConnection;
// 导入 HTTPS 连接类，用于建立安全的 HTTPS 网络连接

import java.net.URL;
// 导入 URL 类，用于表示网络资源的统一资源定位符

public class HttpUtils {
    // 定义公共类 HttpUtils，其他类可以使用这个类


    //分块下载
    //返回值是HttpsURLConnection
    // url: 要下载的网址
    // startPos: 下载的起始位置
    // endPos: 下载的结束位置

    public static  HttpsURLConnection getHttpsURLConnection(String url, long startPos, long endPos ) throws Exception{
             HttpsURLConnection httpsURLConnection =     getHttpsURLConnection( url);
            LogUtils.info("下载的区间是：{}-{}",startPos,endPos == -1 ? "EOF" : endPos);
            
            // ✅ 修复：判断 -1 而不是 0
            if(endPos != -1){
                httpsURLConnection.setRequestProperty("Range", "bytes="+startPos+"-"+endPos);
                LogUtils.info("设置 Range: bytes={}-{}", startPos, endPos);
            }else {
                httpsURLConnection.setRequestProperty("Range", "bytes="+startPos+"-");
                LogUtils.info("设置 Range: bytes={}- (到文件末尾)", startPos);
            }
        return httpsURLConnection;
    }

// 获取 在线下载文件大小
public  static long getHttpsContentLength(String url) throws Exception{
    HttpsURLConnection httpsURLConnection = null;
    try {
        httpsURLConnection = getHttpsURLConnection(url);
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        if (httpsURLConnection != null) {
            httpsURLConnection.disconnect();
        }
    }
    return httpsURLConnection.getContentLength();
    }




    
    // 获取httpsURLConnection
    // 这是一个静态方法，用于创建并返回一个配置好的 HTTPS 连接对象
    public  static HttpsURLConnection getHttpsURLConnection(String url) throws Exception{
        // public: 公共访问权限，任何地方都可以调用
        // static: 静态方法，不需要创建对象就能直接调用
        // HttpsURLConnection: 返回值类型是 HTTPS 连接对象
        // String url: 参数，传入要连接的网址字符串
        // throws Exception: 声明这个方法可能会抛出异常，需要调用者处理
        
        URL httpUrl = new URL(url);//创建URL对象
        // 将传入的字符串 url 转换成 URL 对象
        // URL 类可以解析网址字符串，比如 "https://example.com/file.pdf"
        
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) httpUrl.openConnection();
        // httpUrl.openConnection(): 打开到这个 URL 的网络连接，返回 URLConnection 对象
        // (HttpsURLConnection): 强制类型转换，将 URLConnection 转换为 HttpsURLConnection
        // 因为我们要使用 HTTPS 特有的功能，所以需要转换类型
        // 结果赋值给 httpsURLConnection 变量
        
        //设置请求方式
        // 下面是设置 HTTP 请求的相关属性
        
        //向网站发送请求
        // 设置请求头信息，告诉服务器我们是谁
        
        httpsURLConnection.setRequestProperty("User-Agent",
                // setRequestProperty(): 设置 HTTP 请求头的键值对
                // "User-Agent": 请求头的名称，表示客户端的身份信息
                // 第二个参数是 User-Agent 的值
                
                "Mozilla/5.0 (Windows NT 6.1; WOW64)" +
                        // 模拟浏览器的身份信息第一部分
                        // Mozilla/5.0: 浏览器标识
                        // Windows NT 6.1: Windows 7 操作系统
                        // WOW64: 64位系统上的32位子系统
                        
                        " AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.83"
                // 模拟浏览器的身份信息第二部分
                // AppleWebKit: 浏览器渲染引擎
                // KHTML, like Gecko: 兼容说明
                // Chrome/14.0.83: 模拟 Chrome 14 版本浏览器
                // 这样设置是为了让服务器认为我们是正常浏览器访问，而不是爬虫程序
        );
        // 如果不设置 User-Agent，很多网站会拒绝访问（返回 403 错误）
        
        return httpsURLConnection;
        // 返回配置好的 HTTPS 连接对象
        // 调用者可以用这个连接对象来发送请求、读取响应数据等
        
        
    }
    // 方法结束



    //获取下载的文件名
    // 这是一个静态方法，用于从完整的 URL 中提取文件名
    public  static String getHttpsFileName(String url){
        // public: 公共访问权限
        // static: 静态方法
        // String: 返回值类型是字符串
        // String url: 参数，传入完整的下载地址
        
        return url.substring(url.lastIndexOf("/")+1);
        // url.lastIndexOf("/"): 查找字符串中最后一个 "/" 字符的位置
        // +1: 在最后一个 "/" 的位置上加 1，指向文件名的第一个字符
        // url.substring(...): 从指定位置开始截取到字符串末尾
        // 例如: "https://example.com/files/test.pdf"
        //       lastIndexOf("/") 返回 24（最后一个斜杠的位置）
        //       substring(25) 返回 "test.pdf"
        // return: 返回提取到的文件名
    }
    // 方法结束

}
// 类结束
