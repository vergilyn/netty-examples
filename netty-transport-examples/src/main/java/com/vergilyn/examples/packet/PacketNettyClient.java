package com.vergilyn.examples.packet;

import java.time.LocalTime;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

import static com.vergilyn.examples.common.NettyConstants.INET_HOST;
import static com.vergilyn.examples.common.NettyConstants.NettyThreadPrefix.Packet;


/**
 * @author vergilyn
 * @date 2020-04-01
 */
public class PacketNettyClient {

    public static void main(String[] args) {
        Bootstrap bootstrap = new Bootstrap();
        NioEventLoopGroup group = new NioEventLoopGroup();

        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast("encoder", new StringEncoder());
                    }
                });

        Channel channel = bootstrap.connect(INET_HOST, Packet.inetPort).channel();

        int i = 0, limit = 100;
        do {
            LocalTime now = LocalTime.now();
            String msg = String.format("{index: %d, time: %s}", i, now.toString());
            channel.writeAndFlush(msg);

            System.out.printf("client send: %s \r\n", msg);
        }while (++i < limit);

        channel.disconnect();
    }
}
