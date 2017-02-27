/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.request.impl;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Statements;

/**
 * REST API to manage a transaction.
 *
 * @author Davide D'Alto
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface HttpNeo4jTransactionFacade {

	@POST
	@Path("/transaction")
	Response beginTransaction();

	@POST
	@Path("/transaction/{transactionId}/commit")
	Response commit(@PathParam("transactionId") long transactionId);

	@DELETE
	@Path("/transaction/{transactionId}")
	Response rollback(@PathParam("transactionId") long transactionId);

	@POST
	@Path("/transaction/commit")
	Response executeQuery(Statements statements);

	@POST
	@Path("/transaction/{transactionId}")
	Response executeQuery(@PathParam("transactionId") long transactionId, Statements statements);
}
