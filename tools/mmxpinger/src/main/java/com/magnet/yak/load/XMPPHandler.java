/*   Copyright (c) 2015 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.magnet.yak.load;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class XMPPHandler extends ChannelHandlerAdapter {
	private static final Logger LOGGER = LoggerFactory.getLogger(XMPPHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("CHANNEL READ CALLED");
    	ByteBuf m = (ByteBuf) msg; // (1)
        try {
            long currentTimeMillis = (m.readUnsignedInt() - 2208988800L) * 1000L;
            System.out.println(new Date(currentTimeMillis));
            ctx.close();
        } finally {
            m.release();
        }
    }
    
    @Override
    public void channelActive(final ChannelHandlerContext ctx) { // (1)
        		
    	String s = "<stream:stream to=\"54.148.43.16\" xmlns=\"jabber:client\" xmlns:stream=\"http://etherx.jabber.org/streams\" version=\"1.0\">\"\r\n";
    	final ByteBuf str = ctx.alloc().buffer(s.getBytes().length); // (2)
    	str.writeBytes(s.getBytes());
        LOGGER.trace("channelActive : {}");
        final ChannelFuture f = ctx.writeAndFlush(str);
        f.addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture arg0) throws Exception {
				LOGGER.trace("operationComplete : {}");
			}
            
        });
        /*final ChannelFuture f = ctx.writeAndFlush(time); // (3)
        f.addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture arg0) throws Exception {
				
			}
            
        }); */// (4)
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}