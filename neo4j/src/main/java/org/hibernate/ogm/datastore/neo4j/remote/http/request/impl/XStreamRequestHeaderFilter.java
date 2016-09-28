/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.request.impl;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

/**
 * All responses from the REST API can be transmitted as JSON streams, resulting in better performance and lower memory
 * overhead on the server side.
 *
 * @author Davide D'Alto
 */
public class XStreamRequestHeaderFilter implements ClientRequestFilter {

	public static final XStreamRequestHeaderFilter INSTANCE = new XStreamRequestHeaderFilter();

	private XStreamRequestHeaderFilter() {
	}

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		requestContext.getHeaders().add( "X-Stream", true );
	}
}
