package com.vergilyn.examples.sharable;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.string.StringEncoder;

import static com.vergilyn.examples.common.NettyConstants.INET_HOST;
import static com.vergilyn.examples.common.NettyConstants.NettyThreadPrefix.Sharable;
import static com.vergilyn.examples.common.NettyConstants.newClientBootstrap;

/**
 * @author vergilyn
 * @date 2020-04-01
 */
public class SharableNettyClient {
    public static void main(String[] args) throws InterruptedException {
        Bootstrap bootstrap = newClientBootstrap(1, Sharable);

        bootstrap.handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast("encoder", new StringEncoder());
                    }
                });

        Channel channel1 = bootstrap.connect(INET_HOST, Sharable.inetPort).channel();
        Channel channel2 = bootstrap.connect(INET_HOST, Sharable.inetPort).channel();
        Channel channel3 = bootstrap.connect(INET_HOST, Sharable.inetPort).channel();

        channel1.writeAndFlush("channel-1");
        channel2.writeAndFlush("channel-2");
        channel3.writeAndFlush("channel-3");
    }
}
