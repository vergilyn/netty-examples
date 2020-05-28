package com.vergilyn.examples.handler.idle;

import java.time.LocalTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import static com.vergilyn.examples.common.NettyConstants.INET_HOST;
import static com.vergilyn.examples.common.NettyConstants.NettyThreadPrefix.Handler_IdleState;
import static com.vergilyn.examples.common.NettyConstants.newClientBootstrap;

/**
 * usage {@linkplain IdleStateHandler}
 * @author vergilyn
 * @date 2020-05-28
 */
public class IdleNettyClient {

    public static void main(String[] args) throws InterruptedException {
        Bootstrap bootstrap = newClientBootstrap(1, Handler_IdleState);

        int readerIdleTimeSeconds = 5;
        int writerIdleTimeSeconds = 0;
        int allIdleTimeSeconds = 0;
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline()
                        .addLast("decoder", new StringDecoder())  // inbound
                        .addLast("encoder", new StringEncoder())  // outbound
                        .addLast("client-idle-handler", new IdleStateHandler(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds))
                        .addLast("vergilyn-client-handler", new NettyClientHandler());
            }
        });

        Channel channel = bootstrap.connect(INET_HOST, Handler_IdleState.inetPort).channel();
        TimeUnit.SECONDS.sleep(1);

        String msg = LocalTime.now().toString();
        channel.writeAndFlush(LocalTime.now().toString());

        System.out.printf("client send msg: %s \r\n", msg);

    }

    /**
     * see: dubbo
     *   - `org.apache.dubbo.remoting.transport.netty4.NettyClient#doOpen`
     *   - `org.apache.dubbo.remoting.transport.netty4.NettyClientHandler#userEventTriggered`
     */
    @ChannelHandler.Sharable
    private static class NettyClientHandler extends ChannelDuplexHandler {
        private static final AtomicInteger INDEX = new AtomicInteger(0);

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.printf("client[%s] channelRead() >>>> %s \r\n", LocalTime.now().toString(), msg);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                if (INDEX.incrementAndGet() >= 3){
                    // do nothing, for trigger netty-server idle-state send channel-close
                    return;
                }

                String msg = String.format("[%s]client send heartbeat when read idle.", LocalTime.now().toString());
                ctx.writeAndFlush(msg);

                System.out.println(msg);
            }else {
                super.userEventTriggered(ctx, evt);
            }
        }
    }
}
