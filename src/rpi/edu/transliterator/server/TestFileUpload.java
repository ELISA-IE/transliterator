package rpi.edu.transliterator.server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class TestFileUpload {
    public static void main(String[] args) throws Exception {
        String url = "http://127.0.0.1:10086/train?lang=tha&maxx=3&maxy=3&name=Ying&cutoff=0.0005&data=NEWS";
        URL urlObj = new URL(url);

        HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Accept-Language", "en-US,en");
        con.setRequestProperty("Accept-Charset", "UTF-8");

        con.setDoOutput(true);
        DataOutputStream dos = new DataOutputStream(con.getOutputStream());

        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader
                (new FileInputStream("/home/limteng/project/elisa-ie/transliterator/data/news/pair/thai_en_pairs_train.txt"), "utf-8"));
        while ((line = br.readLine()) != null) {
            dos.write((line + '\n').getBytes());
        }
        br.close();
        dos.flush();
        dos.close();

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        System.out.println(response.toString());
    }
}
