package cookiework.encryptedvideoview.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import static cookiework.encryptedvideoview.Constants.CONNECT_TIMEOUT;
import static cookiework.encryptedvideoview.Constants.READ_TIMEOUT;

/**
 * Created by Administrator on 2017/01/12.
 */

public class HttpUtil {
    private HttpRequestMethod method;
    private String url;
    private String queryString;
    private HttpURLConnection connection;

    public static String convertInputStreamToString(InputStream is) {
        if (is == null) return null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder response = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    public HttpRequestMethod getMethod() {
        return method;
    }

    public HttpUtil setMethod(HttpRequestMethod method) {
        this.method = method;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public HttpUtil setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getQueryString() {
        return queryString;
    }

    public HttpUtil setQueryString(String queryString) {
        this.queryString = queryString;
        return this;
    }

    public HttpUtil setQuery(Map<String, String> query){
        if(query == null || query.isEmpty()){
            queryString = null;
        } else {
            StringBuilder builder = new StringBuilder();
            for (String key : query.keySet()) {
                builder.append(key + "=" + query.get(key) + "&");
            }
            queryString = builder.substring(0, builder.length() - 1);
        }
        return this;
    }

    public void sendHttpRequest() throws Exception {
        if (url == null || method == null) {
            throw new IllegalArgumentException("URL and HTTP Request Method must be set.");
        }

        URL urlObj = new URL(url);
        connection = (HttpURLConnection) urlObj.openConnection();
        if (method == HttpRequestMethod.GET) {
            connection.setRequestMethod("GET");
        } else {
            connection.setRequestMethod("POST");
        }
        if (queryString != null && !queryString.equals("")) {
            connection.setDoOutput(true);
            PrintWriter pw = new PrintWriter(connection.getOutputStream());
            pw.print(queryString);
            pw.flush();
            pw.close();
        }
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
    }

    public enum HttpRequestMethod {GET, POST}

    public int getResponseCode() throws IOException {
        if (connection == null) {
            throw new IllegalArgumentException("Please call sendHttpRequest().first");
        }
        return connection.getResponseCode();
    }

    public String getResponseMessage() throws IOException {
        if (connection == null) {
            throw new IllegalArgumentException("Please call sendHttpRequest().first");
        }
        return connection.getResponseMessage();
    }

    public InputStream getInputStream() throws IOException {
        if (connection == null) {
            throw new IllegalArgumentException("Please call sendHttpRequest().first");
        }
        return connection.getInputStream();
    }

    public InputStream getErrorStream() {
        if (connection == null) {
            throw new IllegalArgumentException("Please call sendHttpRequest().first");
        }
        return connection.getErrorStream();
    }

    public void close() {
        if(connection != null){
            connection.disconnect();
        }
        connection = null;
    }
}
