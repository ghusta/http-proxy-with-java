package fr.husta.test;

import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpProxyTest {

    private String proxyHost = "1.2.3.4";
    private int proxyPort = 8080;

    @Test
    public void testProxyProgrammatically() throws IOException {
        // https://stackoverflow.com/questions/120797/how-do-i-set-the-proxy-to-be-used-by-the-jvm#32897878

        URL testUrl1 = new URL("https://github.com/");
        URL testUrl2 = new URL("https://www.google.fr/");

        HttpURLConnection httpURLConnection;

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        httpURLConnection = (HttpURLConnection) testUrl1.openConnection(proxy);
        // urlConnection = HttpUtils.followRedirects( ... );

        assertThat(httpURLConnection.getResponseCode()).isEqualTo(200);
    }

}
