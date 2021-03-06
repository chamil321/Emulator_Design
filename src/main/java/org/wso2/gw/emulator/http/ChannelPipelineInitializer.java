/*
 * *
 *  * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.wso2.gw.emulator.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import org.wso2.gw.emulator.core.EmulatorType;
import org.wso2.gw.emulator.http.consumer.HttpResponseProcessHandler;
import org.wso2.gw.emulator.http.dsl.consumer.HttpServerBuilde;
import org.wso2.gw.emulator.http.dsl.producer.OutgoingMessage;

public class ChannelPipelineInitializer extends ChannelInitializer<SocketChannel> {

    private SslContext sslCtx;
    private HttpServerBuilde consumerContext;
    private OutgoingMessage producerOutgoingMessage;
    private EmulatorType emulatorType;

    public ChannelPipelineInitializer(SslContext sslCtx, EmulatorType emulatorType) {
        this.sslCtx = sslCtx;
        this.emulatorType = emulatorType;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        if(EmulatorType.HTTP_CONSUMER.equals(emulatorType)) {
            initializeHttpConsumerChannel(ch);
        } else if(EmulatorType.HTTP_PRODUCER.equals(emulatorType)) {
            initializeHttpProducerChannel(ch);
        }
    }

    private void initializeHttpConsumerChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        if (sslCtx != null) {
            pipeline.addLast("sslHandler", sslCtx.newHandler(ch.alloc()));
        }
        pipeline.addLast(new HttpServerCodec());
        if (consumerContext.getLogicHandler() != null) {
            pipeline.addLast("logicHandler", consumerContext.getLogicHandler());
        }
        pipeline.addLast("httpResponseHandler", new HttpResponseProcessHandler(consumerContext));
    }

    private void initializeHttpProducerChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        // Enable HTTPS if necessary.
        if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        }
        pipeline.addLast(new HttpClientCodec());
        pipeline.addLast(new HttpContentDecompressor());
        pipeline.addLast(new org.wso2.gw.emulator.http.producer.HttpResponseProcessHandler(producerOutgoingMessage));
    }

    public void setProducerOutgoingMessage(OutgoingMessage producerOutgoingMessage) {
        this.producerOutgoingMessage = producerOutgoingMessage;
    }

    public void setConsumerContext(HttpServerBuilde consumerContext) {
        this.consumerContext = consumerContext;
    }
}
