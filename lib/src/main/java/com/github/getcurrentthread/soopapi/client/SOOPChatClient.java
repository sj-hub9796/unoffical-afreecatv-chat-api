package com.github.getcurrentthread.soopapi.client;

import com.github.getcurrentthread.soopapi.config.SOOPChatConfig;
import com.github.getcurrentthread.soopapi.decoder.MessageDispatcher;
import com.github.getcurrentthread.soopapi.decoder.factory.DefaultMessageDecoderFactory;
import com.github.getcurrentthread.soopapi.exception.SOOPChatException;
import com.github.getcurrentthread.soopapi.model.ChannelInfo;
import com.github.getcurrentthread.soopapi.model.Message;
import com.github.getcurrentthread.soopapi.util.SOOPChatUtils;
import com.github.getcurrentthread.soopapi.util.SchedulerManager;
import com.github.getcurrentthread.soopapi.websocket.WebSocketListener;
import com.github.getcurrentthread.soopapi.websocket.WebSocketManager;

import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SOOPChatClient implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(SOOPChatClient.class.getName());

    private final SOOPChatConfig config;
    private final WebSocketManager webSocketManager;
    private final List<IChatMessageObserver> observers;
    private final ScheduledExecutorService scheduler;

    public SOOPChatClient(SOOPChatConfig config) {
        this.config = config;
        this.observers = new CopyOnWriteArrayList<>();
        this.scheduler = SchedulerManager.getScheduler();
        MessageDispatcher messageDispatcher = new MessageDispatcher(new DefaultMessageDecoderFactory().createDecoders());
        WebSocketListener listener = new WebSocketListener(messageDispatcher);
        this.webSocketManager = new WebSocketManager(config, config.getSSLContext(), scheduler, listener);

        messageDispatcher.setMessageHandler(this::notifyObservers);
    }

    public void addObserver(IChatMessageObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(IChatMessageObserver observer) {
        observers.remove(observer);
    }

    public CompletableFuture<Void> connectToChat() {
        return CompletableFuture.runAsync(() -> {
            try {
                ChannelInfo channelInfo = SOOPChatUtils.getPlayerLive(config.getBno(), config.getBid());
                if (channelInfo == null) {
                    throw new SOOPChatException("Failed to retrieve channel information.");
                }
                webSocketManager.connect(channelInfo).join();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error occurred while connecting to chat", e);
                throw new CompletionException(e);
            }
        });
    }

    private void notifyObservers(Message message) {
        for (IChatMessageObserver observer : observers) {
            try {
                observer.notify(message);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error notifying observer", e);
            }
        }
    }

    public CompletableFuture<Void> disconnectFromChat() {
        return CompletableFuture.runAsync(() -> {
            webSocketManager.disconnect();
        });
    }

    @Override
    public void close() {
        disconnectFromChat();
        SchedulerManager.releaseScheduler();
    }

    public boolean isConnected() {
        return webSocketManager.isConnected();
    }

    public String getBid() {
        return config.getBid();
    }

    public String getBno() {
        return config.getBno();
    }
}