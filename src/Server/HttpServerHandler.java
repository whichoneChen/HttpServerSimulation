package Server;

import HTTP.HttpRequest;
import HTTP.HttpResponse;
import Server.Resource.ResourceKeeper;

import java.io.*;
import java.util.*;

public class HttpServerHandler {

    private HttpResponse httpResponse;
    private HttpRequest httpRequest;
    private Map<Integer, String> codeAndReason = new HashMap<Integer, String>(){
        {
            codeAndReason.put(200, "OK");
            codeAndReason.put(301, "Moved Permanently");
            codeAndReason.put(302, "Found");
            codeAndReason.put(304, "Not Modified");
            codeAndReason.put(404, "Not Found");
            codeAndReason.put(405, "Method Not Allowed");
            codeAndReason.put(500, "Internal Server Error");
        }
    };
    private ResourceKeeper resourceKeeper = new ResourceKeeper();

    /**
     * ServerHandler的构造方法，对应一个请求报文
     * 考虑到主体部分可能非文字，只能采用【字节流】而不是【字符流】
     * @param data 从socket的【字节流】得到请求报文的【字节】信息
     * @throws IOException
     */
    public HttpServerHandler(byte[] data) throws IOException {
        StringBuffer sb = new StringBuffer();
        char temp;
        int flag = 0;
        boolean isBody = false;

        ArrayList<Byte> body = new ArrayList<>();
        String startLine;
        Map<String, String> headers = new HashMap<>();

        /*
        对字节数组进行转换，在\r\n出现两次的情况认为首部结束，剩下的是主体部分
         */
        for(int i=0; i<data.length; i++){
            if(isBody){
                body.add(data[i]);
            }else {
                temp = (char) data[i];
                if(temp == '\r' || temp == '\n'){
                    flag++;
                }else {
                    flag = 0;
                }
                if(flag == 4){
                    isBody = true;
                }
                sb.append(temp);
            }
        }

        /*
        对开始行和首部信息进行读取，默认每行的结尾都是\r\n
         */
        String[] text = sb.toString().split("\r\n");
        startLine = text[0];
        for (int i=1; i<text.length; i++){
            if(text[i] != ""){
                String[] header = text[i].split(": ");
                headers.put(header[0], header[1]);
            }
        }

        /*
        将主体的Byte[]变成byte[]
        是否有更方便的做法？
         */
        byte[] res = new byte[body.size()];
        for(int i=0; i<body.size(); i++){
            res[i] = body.get(i).byteValue();
        }

        httpRequest = new HttpRequest(startLine, headers, res);
    }

    public HttpResponse process(){
        String method = httpRequest.getMethod();
        switch (method){
            case "GET":
                doGet();
                break;
            case "POST":
                doPost();
                break;
            default:
                do405();
        }
        return this.httpResponse;
    }

    public String getRequestStartLineAndHeaders(){
        return httpRequest.startLineAndHeadersToString();
    }

    public String getResponseStartLineAndHeaders(){
        return httpResponse.startLineAndHeadersToString();
    }

    private void doGet(){
        String url = httpRequest.getUrl();
        String ifModified = httpRequest.getHeader("If-Modified-Since");
        if(ifModified != null){
            do304(new Date());//未完成
        }else {
            String fileName = getFileNameFromUrl(url);
            switch (resourceKeeper.getStatus(fileName)){
                case "valid":
                    if(url == resourceKeeper.getPath(fileName)){
                        do200();
                    }else {
                        do301(resourceKeeper.getPath(fileName));
                    }
                    break;
                case "deleted":
                    do404();
                    break;
                case "temp":
                    do302();
                    break;
            }
        }
    }

    private void doPost(){}

    private void do405(){}

    private void do304(Date sinceDate){}

    private void do301(String newPath){}

    private void do302(){}

    private void do200(){}

    private void do404(){}

    private void do500(){}

    private byte[] readFile(String url){
        File file = new File(url);
        try {
            InputStream in = new FileInputStream(file);
            byte[] body = in.readAllBytes();
            return body;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getFileNameFromUrl(String url){
        String[] t = url.split("/");
        if(t.length == 0){
            return url;
        }else {
            return t[t.length-1];
        }
    }
}
