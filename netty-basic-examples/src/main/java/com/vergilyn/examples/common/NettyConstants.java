package com.vergilyn.examples.common;

import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * @author vergilyn
 * @date 2020-04-01
 */
public interface NettyConstants {
    String INET_HOST = "127.0.0.1";

    enum NettyThreadPrefix{
        Simplex(8080),
        MultiHandler(8081),
        Sharable(8082),
        Packet(8083),
        EventPropagation(8084),
        Duplex(8085),
        Write(8086);

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

    static void initCodecChannel(ServerBootstrap serverBootstrap){
        serverBootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                ch.pipeline().addLast("decoder", new StringDecoder())
                        .addLast("encoder", new StringEncoder());
            }
        });
    }

    static void initCodecHandler(Bootstrap bootstrap){
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                ch.pipeline().addLast("decoder", new StringDecoder())  // inbound
                        .addLast("encoder", new StringEncoder());  // outbound
            }
        });

    }

    static void sleep(int time, TimeUnit unit, String msg){

        try {
            printf("sleep begin %d(%s), %s \r\n", time, unit.name(), msg);
            if (time > 0){
                unit.sleep(time);
            }
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    static void print(String msg){
        printLocalTime();
        System.out.print(msg);
    }

    static void println(String msg){
        printLocalTime();
        System.out.println(msg);
    }

    static void printf(String format, Object... args){
        printLocalTime();
        System.out.printf(format, args);
    }

    static void printLocalTime(){
        System.out.print("<" + LocalTime.now().toString() + "> ");
    }
}
