package com.mojing.novel.config;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;

/** Builds AI clients without accidentally inheriting an IDE or operating-system proxy. */
public final class AiHttpClientFactory {
    private static final ProxySelector DIRECT = new ProxySelector() {
        @Override
        public List<Proxy> select(URI uri) {
            return List.of(Proxy.NO_PROXY);
        }

        @Override
        public void connectFailed(URI uri, java.net.SocketAddress address, IOException error) {
            // A direct connection has no proxy endpoint to report.
        }
    };

    private AiHttpClientFactory() {}

    public static HttpClient create(AiProperties properties) {
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(properties.timeout());
        if (!properties.useSystemProxy()) builder.proxy(DIRECT);
        return builder.build();
    }
}
