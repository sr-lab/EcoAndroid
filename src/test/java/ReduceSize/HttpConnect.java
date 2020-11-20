package test.java.ReduceSize;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpConnect {

    public static void main(String[] args) throws Exception {
        URL url = new URL("http://www.rgagnon.com/howto.html");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        // con.setRequestProperty("Accept-Encoding", "gzip");
        System.out.println("Length : " + con.getContentLength());
        Reader reader = new InputStreamReader(con.getInputStream());
        while (true) {
            int ch = reader.read();
            if (ch==-1) {
                break;
            }
            System.out.print((char)ch);
        }
    }
}