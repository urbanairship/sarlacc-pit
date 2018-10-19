package com.urbanairship.sarlacc.client.util;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.rules.ExternalResource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicReference;

public class TestHttpServer extends ExternalResource {
    private final Server server;
    private final int listenPort;
    private final AtomicReference<Handler> handler = new AtomicReference<Handler>(new AbstractHandler() {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            response.sendError(500, "No handler set!");
        }
    });

    public TestHttpServer() {
        this.listenPort = getAvailablePort();
        this.server = new Server(listenPort);
    }

    private static int getAvailablePort() {
        try (ServerSocket ss = new ServerSocket(0)) {
            ss.setReuseAddress(true);
            return ss.getLocalPort();
        } catch (final IOException e) {
            throw new RuntimeException("Failed to get local port!", e);
        }
    }

    public void setHandler(Handler newHandler) {
        handler.set(newHandler);
    }

    @Override
    protected void before() throws Throwable {
        server.setGracefulShutdown(100);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {
                handler.get().handle(target, baseRequest, request, response);
            }
        });
        server.start();
    }

    @Override
    protected void after() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getListenPort() {
        return listenPort;
    }

    public String getLocalAddr() {
        return "http://localhost:" + listenPort;
    }
}
