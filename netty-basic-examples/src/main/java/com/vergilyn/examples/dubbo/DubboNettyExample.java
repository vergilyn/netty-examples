package com.vergilyn.examples.dubbo;

import com.vergilyn.examples.common.NettyEventLoopFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;

/**
 * <a href="https://github.com/apache/dubbo/blob/master/dubbo-remoting/dubbo-remoting-netty4/src/main/java/org/apache/dubbo/remoting/transport/netty4/NettyServer.java">
 * dubbo netty4 `NettyServer.java`
 * </a>
 *
 * @author vergilyn
 * @date 2020-03-23
 */
public class DubboNettyExample {
    public static void main(String[] args) {
    }

    /**
     * <a href="https://github.com/apache/dubbo/blob/2.7.6-release/dubbo-remoting/dubbo-remoting-netty4/src/main/java/org/apache/dubbo/remoting/transport/netty4/NettyServer.java">
     *   dubbo, v2.7.6.relese, NettyServer.java
     * </a>
     */
    private static void server() {
        ServerBootstrap bootstrap = new ServerBootstrap();

        // vergilyn-comment, 2020-03-13 >>>> 经典的 netty多线程模型 - Reactor
        EventLoopGroup bossGroup = NettyEventLoopFactory.eventLoopGroup(1, "NettyServerBoss");
        EventLoopGroup workerGroup = NettyEventLoopFactory.eventLoopGroup(NettyEventLoopFactory.DEFAULT_IO_THREADS, "NettyServerWorker");

        bootstrap.group(bossGroup, workerGroup)
                .channel(NettyEventLoopFactory.serverSocketChannelClass())
                .option(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast("handler", new CustomNettyServerHandler());
                    }
                });
        // bind
        ChannelFuture channelFuture = bootstrap.bind("127.0.0.1", 8085);
        channelFuture.syncUninterruptibly();
        Channel channel = channelFuture.channel();
    }

    private static void client() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(NettyEventLoopFactory.eventLoopGroup(NettyEventLoopFactory.DEFAULT_IO_THREADS, "NettyClientWorker"))
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                //.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getTimeout())
                .channel(NettyEventLoopFactory.socketChannelClass());

        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new CustomNettyClientHandler());
            }
        });
    }

    private static class CustomNettyClientHandler extends ChannelDuplexHandler {
    }

    private static class CustomNettyServerHandler extends ChannelDuplexHandler {
    }
}
