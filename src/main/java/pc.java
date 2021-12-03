import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author tao
 * @createTime 2021/11/29 22:55
 * @description
 */
public class pc {

    /**
     * 抓包token
     */
    private static final String ZSXQ_ACCESS_TOKEN = "97E31117-F75B-D316-8E6D-AA36EBE9E879_610B63E6232A4560";
    /**
     * 文件保存路径
     */
    private static final String PATH = "/Users/tao/IdeaProjects/爬虫/大余/";
    public static List<String> fileIdList = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        // 设置线程池数量
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "1");
        //圈子ID
        String groupId = "51284855218444";
        //获取文件id
        List<String> idList = getFileIdList(ZSXQ_ACCESS_TOKEN, groupId, "");
        //下载文件
        idList.parallelStream().forEach(fileId -> {
            downFile(ZSXQ_ACCESS_TOKEN, fileId, PATH);
        });
        System.out.println("本次一共下载:" + idList.size() + "个文件");


    }


    /**
     * @param downlodaUrl 下载地址
     * @param path        保存路径
     */
    private static void download(String downlodaUrl, String path) {
        try {
            //获取用户名
            String filename = URLUtil.decode(StrUtil.subBetween(downlodaUrl, "attname=", "&"), "utf-8");
            System.out.println("文件名" + filename);
            //long file = HttpUtil.downloadFile(downlodaUrl,path+filename);
            HttpUtil.downloadFile(downlodaUrl, FileUtil.file(path+filename), new StreamProgress(){
                @Override
                public void start() {
                    Console.log("开始下载。。。。");
                }

                @Override
                public void progress(long progressSize) {
                    Console.log("已下载：{"+filename+"}", FileUtil.readableFileSize(progressSize));
                }

                @Override
                public void finish() {
                    Console.log("下载完成！");
                }
            });
        } catch (Exception e) {
            System.out.println(downlodaUrl);
        }
    }

    /**
     * @param zsxq_access_token 圈子token
     * @param groupID           圈子ID
     * @param end_time          最后时间
     * @return
     */
    private static List<String> getFileIdList(String zsxq_access_token, String groupID, String end_time) {
        try {
            String url = "https://api.zsxq.com/v2/groups/" + groupID + "/files?count=20";
            //设置响应头
            String body = SetHandler(url, zsxq_access_token, end_time);
            JSONArray jsonArray = JSONUtil.parseArray(JSONUtil.parseObj(body).getJSONObject("resp_data").getStr("files"));
            if (JSONUtil.parseObj(body).getStr("succeeded") == "false") {
                getFileIdList(zsxq_access_token, groupID, end_time);
            }
            //取出时间
            String temp = JSONUtil.parseObj(jsonArray.get(jsonArray.size() - 1).toString()).getJSONObject("file").getStr("create_time");
            temp = URLEncoder.encode(temp, "UTF-8");
            if (!temp.equals(end_time)) {
                end_time = temp;
                getFileIdList(zsxq_access_token, groupID, end_time);
            }

            // 循环遍历id
            for (Object object : jsonArray) {
                String file_id = JSONUtil.parseObj(object.toString()).getJSONObject("file").getStr("file_id");
                fileIdList.add(file_id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        fileIdList = fileIdList.stream().distinct().collect(Collectors.toList());
        System.out.println("圈子id：" + fileIdList);
        return fileIdList;

    }

    /**
     * @param zsxq_access_token 圈子token
     * @param fileID            文件id
     * @param path              下载路径
     */
    private static void downFile(String zsxq_access_token, String fileID, String path) {
        String url = "https://api.zsxq.com/v2/files/" + fileID + "/download_url";
        //定义请求头信息
        //链式请求
        String result = HttpRequest.get(url)
                .header(Header.COOKIE, "abtest_env=product; zsxq_access_token=" + zsxq_access_token)
                .header(Header.USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36")
                .timeout(20000)
                .execute().body();

        try {
            if (JSONUtil.parseObj(result).getStr("succeeded") == "false") {
                downFile(zsxq_access_token,fileID,path);
            }
            String downlodaUrl = JSONUtil.parseObj(result).getJSONObject("resp_data").getStr("download_url");
            //解析文件名
            System.out.println("下载地址：" + downlodaUrl);
            //下载文件
            download(downlodaUrl, path);
        } catch (Exception e) {

        }
    }

    /**
     * @param url               网站
     * @param zsxq_access_token token
     * @param end_time          结束时间
     * @return
     */
    public static String SetHandler(String url, String zsxq_access_token, String end_time) {
        if (!"".equals(end_time)) {
            url = url + "&end_time=" + end_time;
            System.out.println("请求的url" + url);
        }
        try {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("cookie", "abtest_env=product; zsxq_access_token=" + zsxq_access_token);
            httpGet.setHeader("user-agent",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36");
            CloseableHttpResponse response = httpclient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String body = EntityUtils.toString(entity, "utf-8");
            return body;
        } catch (ClientProtocolException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return "ERROR";

    }

}
