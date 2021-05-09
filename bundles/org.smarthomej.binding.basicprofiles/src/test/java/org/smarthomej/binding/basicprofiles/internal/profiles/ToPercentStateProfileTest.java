/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.binding.basicprofiles.internal.profiles;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.PercentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class ToPercentStateProfileTest {

    Logger logger = LoggerFactory.getLogger(ToPercentStateProfileTest.class);

    @Test
    void test() {
        logger.info("DecmalType(100): {} ({})", new DecimalType(100.0), new DecimalType(100.0).doubleValue());
        logger.info("PercentType(100): {} ({})", PercentType.HUNDRED, PercentType.HUNDRED.doubleValue());

        logger.info("DecmalType(50): {} ({})", new DecimalType(50.0), new DecimalType(50.0).doubleValue());
        logger.info("PercentType(50): {} ({})", new PercentType(50), new PercentType(50).doubleValue());
    }
}
