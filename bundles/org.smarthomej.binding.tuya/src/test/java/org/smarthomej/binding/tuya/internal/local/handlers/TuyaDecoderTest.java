/**
 * Copyright (c) 2021-2023 Contributors to the SmartHome/J project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.smarthomej.binding.tuya.internal.local.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.smarthomej.binding.tuya.internal.local.ProtocolVersion.V3_4;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.util.HexUtils;
import org.smarthomej.binding.tuya.internal.local.MessageWrapper;
import org.smarthomej.binding.tuya.internal.local.TuyaDevice;

import com.google.gson.Gson;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * The {@link TuyaDecoderTest} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class TuyaDecoderTest {

    private final Gson gson = new Gson();
    private @Mock @NonNullByDefault({}) ChannelHandlerContext ctx;
    private @Mock @NonNullByDefault({}) Channel channelMock;

    @Test
    public void decode34Test() throws Exception {
        when(ctx.channel()).thenReturn(channelMock);

        TuyaDevice.KeyStore keyStore = new TuyaDevice.KeyStore("5c8c3ccc1f0fbdbb".getBytes(StandardCharsets.UTF_8));
        byte[] packet = HexUtils.hexToBytes(
                "000055aa0000fc6c0000000400000068000000004b578f442ec0802f26ca6794389ce4ebf57f94561e9367569b0ff90afebe08765460b35678102c0a96b666a6f6a3aabf9328e42ea1f29fd0eca40999ab964927c340dba68f847cb840b473c19572f8de9e222de2d5b1793dc7d4888a8b4f11b00000aa55");
        byte[] expectedResult = HexUtils.hexToBytes(
                "3965333963353564643232333163336605ca4f27a567a763d0df1ed6c34fa5bb334a604d900cc86b8085eef6acd0193d");

        List<Object> out = new ArrayList<>();

        TuyaDecoder decoder = new TuyaDecoder(gson, "", keyStore, V3_4);
        decoder.decode(ctx, Unpooled.copiedBuffer(packet), out);

        assertThat(out, hasSize(1));
        MessageWrapper<?> result = (MessageWrapper<?>) out.get(0);
        assertThat(result.content, is(expectedResult));
    }
}
