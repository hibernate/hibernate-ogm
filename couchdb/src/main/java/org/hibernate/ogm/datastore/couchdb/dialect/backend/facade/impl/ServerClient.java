/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.backend.facade.impl;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * *
 * The Interface used by RESTEasy to create the REST calls used to
 * interact with the CouchDB server
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 */
@Path("/")
@Produces("application/json")
@Consumes("application/json")
public interface ServerClient {

	/**
	 * Create a new database
	 *
	 * @param databaseName
	 *            the name of the new database
	 * @return the {@link Response}
	 */
	@PUT
	@Path("{name}")
	Response createDatabase(@PathParam("name") String databaseName);

	/**
	 * Retrieve the name of all the databases on the CouchDBServer
	 *
	 * @return the ClientResponse with the list of the names of the existing databases
	 */
	@GET
	@Path("_all_dbs")
	Response getAllDatabaseNames();
}
