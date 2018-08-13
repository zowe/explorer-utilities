/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.utilities;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

public class Utils {

	static public String generateRandomString() {
		return UUID.randomUUID().toString();
	}

	public static Date getOldDateUTCFromLocalDate(LocalDate localDate) {
		return Date.from(localDate.atStartOfDay(ZoneId.of("UTC")).toInstant());
	}
}
