/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.utilities.client;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

public class HTTPClientProducer {

	private static final String CLIENT_EXCEPTION = "Exception occured while creating client to send request";

	@Context
	UriInfo uriInfo;

	@Produces
	public HTTPClient getInstance() throws WebApplicationException {
		try {
			return new HTTPClient(uriInfo);
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			String error = String.format("%s : %s", CLIENT_EXCEPTION, e.getMessage());
			throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
		}
	}

	public void close(@Disposes HTTPClient client) {
		client.close();
	}
}
