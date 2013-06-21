/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.dialect.couchdb.resteasy;

import org.jboss.resteasy.client.ClientResponse;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.List;

/**
 * *
 * The Interface used by RESTEasy to create the REST calls used to
 * interact with the CouchDB server
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
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
	 * @return the {@link ClientResponse}
	 */
	@PUT
	@Path("{name}")
	ClientResponse<CouchDBResponse> createDatabase(@PathParam("name") String databaseName);

	/**
	 * Retrieve the name of all the databases on the CouchDBServer
	 *
	 * @return the ClientResponse with the list of the names of the existing databases
	 */
	@GET
	@Path("_all_dbs")
	ClientResponse<List<String>> getAllDatabaseNames();
}
