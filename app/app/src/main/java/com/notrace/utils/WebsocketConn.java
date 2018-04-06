package com.notrace.utils;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.notrace.config.AppConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.ws.WebSocket;
import okhttp3.ws.WebSocketCall;
import okhttp3.ws.WebSocketListener;
import okio.Buffer;

/**
 * Created by paron on 2018/3/24.
 */

public class WebsocketConn {
    private static final String LOGTAG = "websocket";
    private static WebSocket _webSocket = null;

    private static Handler handler = null;

    public static void conn(String url) {
        if(handler!=null) {
            handler.getLooper().quit();
        }
        final HandlerThread handlerThread = new HandlerThread("websocket");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.MINUTES)
                .writeTimeout(30, TimeUnit.MINUTES)
                .connectTimeout(30, TimeUnit.MINUTES)
                .build();
        final Request request = new Request.Builder()
                .url(url)
                .build();
        handler.post(new Runnable() {
            @Override
            public void run() {
                WebSocketCall webSocketCall = WebSocketCall.create(okHttpClient, request);
                webSocketCall.enqueue(new WebSocketListener() {
                    @Override
                    public void onOpen(WebSocket webSocket, Response response) {
                        //保存引用，用于后续操作
                        _webSocket = webSocket;
                        //打印一些内容
                        System.out.println("client onOpen");
                        System.out.println("client request header:" + response.request().headers());
                        System.out.println("client response header:" + response.headers());
                        System.out.println("client response:" + response);

                        try {
                            webSocket.sendPing(null);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(IOException e, Response response) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onMessage(ResponseBody message) throws IOException {
                        String content = message.string();
                        Log.e(LOGTAG, content);
                    }

                    @Override
                    public void onPong(Buffer payload) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    _webSocket.sendPing(null);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    retryDelay();
                                }
                            }
                        },60*1000);
                    }

                    @Override
                    public void onClose(int code, String reason) {
                    }
                });
            }
        });
    }

    private static void retryDelay() {
        try {
            _webSocket.close(0,"ping failed");
        } catch (Exception e) {
            e.printStackTrace();
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                conn(AppConfig.getWebsocketUrl());
            }
        },10*1000);
    }

    public static void sendMessage(final String message) {
        if(handler!=null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        _webSocket.sendMessage(RequestBody.create(WebSocket.TEXT, message));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
