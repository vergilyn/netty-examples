package com.vergilyn.examples.common;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @author vergilyn
 * @date 2020-04-01
 */
public interface NettyConstants {
    String INET_HOST = "127.0.0.1";

    int INET_PORT_PACKET = 8082;

    enum NettyThreadPrefix{
        Basic(8080),
        MultiHandler(8081),
        Sharable(8082),
        Packet(8083),
        EventPropagation(8085);

        public final int inetPort;

        NettyThreadPrefix(int inetPort) {
            this.inetPort = inetPort;
        }
    }

    static ServerBootstrap newServerBootstrap(int bossThreads, int workThreads, NettyThreadPrefix prefix){
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        EventLoopGroup boss = NettyEventLoopFactory.eventLoopGroup(bossThreads, prefix.name() + "NettyServerBoss");
        EventLoopGroup worker = NettyEventLoopFactory.eventLoopGroup(workThreads, prefix.name() + "NettyServerWork");

        return serverBootstrap.group(boss, worker).channel(NioServerSocketChannel.class);
    }

    static Bootstrap newClientBootstrap(int threads, NettyThreadPrefix prefix){
        Bootstrap bootstrap = new Bootstrap();
        EventLoopGroup group = NettyEventLoopFactory.eventLoopGroup(threads, prefix.name() + "NettyClient");

        return bootstrap.group(group).channel(NioSocketChannel.class);
    }


}
