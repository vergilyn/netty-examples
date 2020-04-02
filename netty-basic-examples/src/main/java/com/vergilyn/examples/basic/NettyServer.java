package com.vergilyn.examples.basic;

import com.vergilyn.examples.common.NettyEventLoopFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

import static com.vergilyn.examples.common.NettyConstants.NettyThreadPrefix.Basic;

/**
 * @author VergiLyn
 * @date 2019-04-15
 */
public class NettyServer {
    public static void main(String[] args) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        EventLoopGroup boss = NettyEventLoopFactory.eventLoopGroup(1, "NettyServerBoss");
        EventLoopGroup worker = NettyEventLoopFactory.eventLoopGroup(NettyEventLoopFactory.DEFAULT_IO_THREADS, "NettyServerWork");

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
                .bind(Basic.inetPort);// 4. 绑定端口
    }
}
