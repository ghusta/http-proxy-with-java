package fr.husta.test;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.junit.Test;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URL;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class HttpProxyTest {

    private String proxyHost = "local-docker";
    private int proxyPort = 9580;

    private static final URL TEST_URL1;
    private static final URL TEST_URL2;
    private static final URL TEST_URL_SIMPLE_HTTP;

    static {
        try {
            TEST_URL1 = new URL("https://github.com/");
        } catch (MalformedURLException e) {
            throw new ExceptionInInitializerError(e);
        }

        try {
            TEST_URL2 = new URL("https://www.google.fr/");
        } catch (MalformedURLException e) {
            throw new ExceptionInInitializerError(e);
        }

        try {
            TEST_URL_SIMPLE_HTTP = new URL("http://www.mysimpleserver.com/");
        } catch (MalformedURLException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Test
    public void testProxyProgrammaticallyWithHttp() throws IOException {
        // https://stackoverflow.com/questions/120797/how-do-i-set-the-proxy-to-be-used-by-the-jvm#32897878

        // No need of certificate if not using HTTPS

        HttpURLConnection httpURLConnection;

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        httpURLConnection = (HttpURLConnection) TEST_URL_SIMPLE_HTTP.openConnection(proxy);
        // urlConnection = HttpUtils.followRedirects( ... );

        assertThat(httpURLConnection.getResponseCode()).isEqualTo(200);
    }

    @Test
    public void testProxyProgrammatically() throws IOException {
        // https://stackoverflow.com/questions/120797/how-do-i-set-the-proxy-to-be-used-by-the-jvm#32897878

        // Add system properties for accessing HTTPS with an imported certificate
        System.setProperty("javax.net.ssl.trustStore", "./cacerts/mitmproxycacert");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

        HttpURLConnection httpURLConnection;

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        httpURLConnection = (HttpURLConnection) TEST_URL1.openConnection(proxy);
        // urlConnection = HttpUtils.followRedirects( ... );
        assertThat(httpURLConnection.getResponseCode()).isEqualTo(200);

        httpURLConnection = (HttpURLConnection) TEST_URL2.openConnection(proxy);
        assertThat(httpURLConnection.getResponseCode()).isEqualTo(200);
    }

    @Test
    public void testProxyProgrammaticallyWithHttps() throws IOException {
        // https://stackoverflow.com/questions/120797/how-do-i-set-the-proxy-to-be-used-by-the-jvm#32897878

        // Add system properties for accessing HTTPS with an imported certificate
        System.setProperty("javax.net.ssl.trustStore", "./cacerts/mitmproxycacert");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

        HttpsURLConnection httpsURLConnection;

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        httpsURLConnection = (HttpsURLConnection) TEST_URL1.openConnection(proxy);
        assertThat(httpsURLConnection.getResponseCode()).isEqualTo(200);

        // more options with HttpsURLConnection and SSL
        Certificate[] serverCertificates = httpsURLConnection.getServerCertificates();
        assertThat(serverCertificates).isNotEmpty();

        X509Certificate[] x509Certificates = (X509Certificate[]) serverCertificates;
        List<String> subjects = Arrays.stream(x509Certificates)
                .map(X509Certificate::getSubjectDN)
                .map(Principal::getName)
                .collect(toList());
        assertThat(subjects).contains("O=mitmproxy, CN=mitmproxy", "CN=github.com");
        // same as preceding
        assertThat(serverCertificates).extracting("SubjectDN.name").contains("O=mitmproxy, CN=mitmproxy", "CN=github.com");

        List<String> issuers = Arrays.stream(x509Certificates)
                .map(X509Certificate::getIssuerDN)
                .map(Principal::getName)
                .collect(toList());
        assertThat(issuers).containsOnly("O=mitmproxy, CN=mitmproxy");
    }

    @Test
    public void testJavaNetProxySelector() {
        String propUseSystemProxies = System.getProperty("java.net.useSystemProxies");
        System.out.println("propUseSystemProxies = " + propUseSystemProxies);

        ProxySelector proxySelector;

        proxySelector = ProxySelector.getDefault();
        assertThat(proxySelector).isNotNull();
        System.out.println(proxySelector);
    }

    /**
     * Test using library <b>HTTPComponents Client</b>.
     *
     * @throws IOException
     */
    @Test
    public void testProxyWithApacheHttpComponents() throws IOException {
        // see : HttpClient proxy configuration (https://hc.apache.org/httpcomponents-client-4.5.x/tutorial/html/connmgmt.html#d5e485)

        // Add system properties for accessing HTTPS with an imported certificate
        System.setProperty("javax.net.ssl.trustStore", "./cacerts/mitmproxycacert");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

        HttpHost proxy = new HttpHost(proxyHost, proxyPort);
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
        CloseableHttpClient httpclient = HttpClients.custom()
                .setRoutePlanner(routePlanner)
                .build();

        HttpUriRequest request = new HttpGet(TEST_URL1.toString());
        CloseableHttpResponse closeableHttpResponse = httpclient.execute(request);
        assertThat(closeableHttpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
    }

}
