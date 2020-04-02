package com.vergilyn.examples.packet;

import java.util.concurrent.atomic.AtomicInteger;

import com.vergilyn.examples.common.NettyEventLoopFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import lombok.extern.slf4j.Slf4j;

import static com.vergilyn.examples.common.NettyConstants.NettyThreadPrefix.Packet;


/**
 * @author vergilyn
 * @date 2020-04-01
 */
public class PacketNettyServer {

    public static void main(String[] args) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        EventLoopGroup boss = NettyEventLoopFactory.eventLoopGroup(1, "PacketNettyServerBoss");
        EventLoopGroup worker = NettyEventLoopFactory.eventLoopGroup(1, "PacketNettyServerWork");

        final PacketChannelHandler packetChannelHandler = new PacketChannelHandler();
        ChannelFuture channelFuture = serverBootstrap.group(boss, worker)    // 1. 线程模型
                .channel(NioServerSocketChannel.class)  // 2. IO模型
                .childHandler(new ChannelInitializer<NioSocketChannel>() {  // 3. 连接读写处理逻辑
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline()
                                .addLast("delimiter-based-frame-decoder", new DelimiterBasedFrameDecoder(40,
                                            Unpooled.wrappedBuffer("}".getBytes())))  // 解决 粘包
                                .addLast("decoder", new StringDecoder())
                                .addLast("packet-handler", packetChannelHandler);
                    }
                })
                .bind(Packet.inetPort);// 4. 绑定端口

    }

    @Slf4j
    @ChannelHandler.Sharable
    static class PacketChannelHandler extends ChannelInboundHandlerAdapter {
        // 接收消息计数器
        private AtomicInteger index = new AtomicInteger(0);

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            index = new AtomicInteger(0);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            int i = index.getAndIncrement();

            // 对每条读取到的消息进行打数标记
            System.out.printf("[Thread-%s-%d]server receive[%d]: %s \r\n",
                    Thread.currentThread().getName(),
                    Thread.currentThread().getId(), i, msg);

            ctx.write(msg);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            // ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            System.out.print("exceptionCaught() >>>>");
            cause.printStackTrace();
        }
    }
}
