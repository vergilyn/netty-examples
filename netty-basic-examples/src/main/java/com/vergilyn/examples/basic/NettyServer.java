package com.vergilyn.examples.basic;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

/**
 * @author VergiLyn
 * @date 2019-04-15
 */
public class NettyServer {
    public static final int INET_PORT = 8080;

    public static void main(String[] args) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        ChannelFuture channelFuture = serverBootstrap.group(boss, worker)    // 1. 线程模型
                .channel(NioServerSocketChannel.class)  // 2. IO模型
                .childHandler(new ChannelInitializer<NioSocketChannel>() {  // 3. 连接读写处理逻辑
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline()
                                .addLast(new StringDecoder())
                                .addLast(new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                        System.out.println(msg);
                                    }
                                });
                    }
                })
                .bind(INET_PORT);// 4. 绑定端口
    }
}
