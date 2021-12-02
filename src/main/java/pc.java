import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

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

    public static List<String> fileIdList = new CopyOnWriteArrayList<>();

    /**
     * 抓包token
     */
    private static final String ZSXQ_ACCESS_TOKEN = "D9BE997B-9344-0F1B-5A34-FF5FFEFB0C57_610B63E6232A4560";

    /**
     * 文件保存路径
     */
    private static final String PATH = "/Users/tao/IdeaProjects/爬虫/大余/";

    public static void main(String[] args) {
        // 设置线程池数量
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "20");
        //圈子ID
        String groupId = "51284855218444";

        //获取文件id
        List<String> idList = getFileIdList(ZSXQ_ACCESS_TOKEN, groupId, "");

        //下载文件
        idList.parallelStream().forEach(fileId -> {
            downFile(ZSXQ_ACCESS_TOKEN, fileId, PATH);
        });
        System.out.println("圈子id2：" + fileIdList);
        System.out.println("本次一共下载:" + idList.size() + "个文件");


    }

    /**
     * @param zsxq_access_token 圈子token
     * @param fileID            文件id
     * @param path              下载路径
     */
    private static void downFile(String zsxq_access_token, String fileID, String path) {
        //下载路径https://api.zsxq.com/v2/files/544815581841524/download_url
        String url = "https://api.zsxq.com/v2/files/" + fileID + "/download_url";
        //定义请求头信息
        //链式请求
        String result = HttpRequest.get(url)
                .header(Header.COOKIE, "zsxq_access_token=" + zsxq_access_token)
                .header(Header.USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36")
                .timeout(20000)
                .execute().body();

        String downlodaUrl = JSONUtil.parseObj(result).getJSONObject("resp_data").getStr("download_url");
        //解析文件名
        System.out.println("下载地址：" + downlodaUrl);
        //下载文件
        download(downlodaUrl,path);
    }

    /**
     *
     * @param downlodaUrl 下载地址
     * @param path 保存路径
     */
    private static void download(String downlodaUrl, String path) {
        try {
           //获取用户名
            String filename = URLUtil.decode(StrUtil.subBetween(downlodaUrl,"attname=","&"),"utf-8");
            System.out.println("文件名"+filename);
            long file = HttpUtil.downloadFile(downlodaUrl,path+filename);
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
        //https://api.zsxq.com/v2/groups/51284855218444/files?count=20
        String url = "https://api.zsxq.com/v2/groups/" + groupID + "/files?count=20";
        //设置响应头
        String body = SetHandler(url, zsxq_access_token, end_time);
        JSONArray jsonArray = JSONUtil.parseArray(JSONUtil.parseObj(body).getJSONObject("resp_data").getStr("files"));
        System.out.println("测试数据" + jsonArray);
        //取出时间

        String temp = JSONUtil.parseObj(jsonArray.get(jsonArray.size() - 1).toString()).getJSONObject("file").getStr("create_time");
        //https://api.zsxq.com/v2/groups/51284855218444/files?count=20&end_time=2021-06-14T09:23:21.643+0800
        //temp=StrUtil.replace(temp,":","%3A").replace("+","%2B");
        System.out.println("原来的时间"+temp);

        try {
            temp = URLEncoder.encode(temp,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        System.out.println("这是时间编码格式化"+temp);
        if (!temp.equals(end_time)){
            end_time=temp;
            getFileIdList(zsxq_access_token,groupID,end_time);
        }
        // 循环遍历id
        for (Object object : jsonArray) {
            String file_id = JSONUtil.parseObj(object.toString()).getJSONObject("file").getStr("file_id");
            fileIdList.add(file_id);
        }
        fileIdList = fileIdList.stream().distinct().collect(Collectors.toList());
        System.out.println("圈子id：" + fileIdList);
        return fileIdList;

    }

    public static String SetHandler(String url, String zsxq_access_token, String end_time) {
        if (!"".equals(end_time)) {
            url = url + "&end_time=" + end_time;
            System.out.println("请求的url"+url);
        }
        //链式请求

        return HttpRequest.get(url)
                .header(Header.COOKIE, "abtest_env=product; zsxq_access_token=" + zsxq_access_token)
                .header(Header.USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36")
                .timeout(20000)
                .setFollowRedirects(true)
                .execute().body();
    }

}
