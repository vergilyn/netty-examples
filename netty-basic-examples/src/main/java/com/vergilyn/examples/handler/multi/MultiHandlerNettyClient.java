package com.vergilyn.examples.handler.multi;

import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

/**
 * @author vergilyn
 * @date 2020-03-23
 */
public class MultiHandlerNettyClient {

    public static void main(String[] args) throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        NioEventLoopGroup group = new NioEventLoopGroup();

        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(new StringEncoder());
                    }
                });

        Channel channel = bootstrap.connect(MultiHandlerNettyServer.INET_HOST, MultiHandlerNettyServer.INET_PORT).channel();

        channel.writeAndFlush(MultiHandlerNettyClient.class.getSimpleName() + " send: " + LocalTime.now().toString());
        TimeUnit.SECONDS.sleep(10);
    }
}
