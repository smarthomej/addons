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
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.smarthomej.binding.tuya.internal.local.ProtocolVersion.V3_4;

import java.nio.charset.StandardCharsets;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.util.HexUtils;
import org.smarthomej.binding.tuya.internal.local.CommandType;
import org.smarthomej.binding.tuya.internal.local.MessageWrapper;
import org.smarthomej.binding.tuya.internal.local.TuyaDevice;

import com.google.gson.Gson;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * The {@link TuyaEncoderTest} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class TuyaEncoderTest {

    private final Gson gson = new Gson();
    private @Mock @NonNullByDefault({}) ChannelHandlerContext ctx;
    private @Mock @NonNullByDefault({}) ByteBuf out;

    @Test
    public void testEncoding34() throws Exception {
        TuyaDevice.KeyStore keyStore = new TuyaDevice.KeyStore("5c8c3ccc1f0fbdbb".getBytes(StandardCharsets.UTF_8));
        byte[] payload = HexUtils.hexToBytes("47f877066f5983df0681e1f08be9f1a1");
        byte[] expectedResult = HexUtils.hexToBytes(
                "000055aa000000010000000300000044af06484eb01c2272666a10953aaa23e89328e42ea1f29fd0eca40999ab964927c99646647abb2ab242062a7e911953195ae99b2ee79fa00a95da8cc67e0b42e20000aa55");

        MessageWrapper<?> msg = new MessageWrapper<>(CommandType.SESS_KEY_NEG_START, payload);

        TuyaEncoder encoder = new TuyaEncoder(gson, "", keyStore, V3_4);
        encoder.encode(ctx, msg, out);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

        verify(out).writeBytes((byte[]) captor.capture());
        byte[] result = (byte[]) captor.getValue();
        assertThat(result.length, is(expectedResult.length));
        assertThat(result, is(expectedResult));
    }
}
