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

import org.hibernate.ogm.dialect.couchdb.designdocument.AssociationsDesignDocument;
import org.hibernate.ogm.dialect.couchdb.designdocument.EntitiesDesignDocument;
import org.hibernate.ogm.dialect.couchdb.designdocument.EntityTupleRows;
import org.hibernate.ogm.dialect.couchdb.designdocument.Rows;
import org.hibernate.ogm.dialect.couchdb.designdocument.TuplesDesignDocument;
import org.jboss.resteasy.client.ClientResponse;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * The Interface used by RESTEasy to create the REST calls used to
 * interact with the CouchDB database instance
 *
 * The methods return a ClientResponse<T> instead directly the T instance in order to have more information in case of
 * failures
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
@Path("/")
@Produces("application/json")
@Consumes("application/json")
public interface DatabaseClient {

	/**
	 * Save a {@link CouchDBDocument} with the given id
	 *
	 * @param document
	 *            to be saved
	 * @param id
	 *            to use for the document
	 * @return the {@link ClientResponse} from CouchDB
	 */
	@PUT
	@Path("{id}")
	ClientResponse<CouchDBResponse> saveDocument(CouchDBDocument document, @PathParam("id") String id);

	/**
	 * Delete the CouchDBDocument with the given id and revision
	 *
	 * @param id
	 *            of the CouchDBDocument to delete
	 * @param revision
	 *            of the CouchDBDocument to delete
	 * @return the ClientResponse
	 */
	@DELETE
	@Path("{id}")
	ClientResponse<CouchDBResponse> deleteDocument(@PathParam("id") String id, @QueryParam("rev") String revision);

	/**
	 * Delete the Database from CouchDB
	 *
	 * @return the ClientResponse
	 */
	@DELETE
	@Path("/")
	ClientResponse dropDatabase();

	/**
	 * Retrieve the {@link CouchDBEntity} with the given id
	 *
	 * @param id
	 *            of the CouchDBEntity to retrieve
	 * @return the ClientResponse with the searched CouchDBEntity
	 */
	@GET
	@Path("{id}")
	ClientResponse<CouchDBEntity> getEntityId(@PathParam("id") String id);

	/**
	 * Retrieve the {@link CouchDBAssociation} with the given id
	 *
	 * @param id
	 *            of the CouchDBAssociation to retrieve
	 * @return the ClientResponse with the searched CouchDBAssociation
	 */
	@GET
	@Path("{id}")
	ClientResponse<CouchDBAssociation> getAssociationById(@PathParam("id") String id);

	/**
	 * Retrieve the {@link CouchDBKeyValue} with the given id
	 *
	 * @param id
	 *            of the CouchDBKeyValue to retrieve
	 * @return the ClientResponse with the searched CouchDBKeyValue
	 */
	@GET
	@Path("{id}")
	ClientResponse<CouchDBKeyValue> getKeyValue(@PathParam("id") String id);

	/**
	 * Retrieve the number of CouchDBAssociation stored in the database
	 *
	 * @return the ClientResponse with the searched CouchDBKeyValue
	 */
	@GET
	@Path(AssociationsDesignDocument.NUMBER_OF_ASSOCIATIONS_VIEW_PATH)
	ClientResponse<Rows> getNumberOfAssociations();

	/**
	 * Retrieve the number of CouchDBEntity stored in the database
	 *
	 * @return the ClientResponse CouchDB with the {@link Rows}
	 */
	@GET
	@Path(EntitiesDesignDocument.NUMBER_OF_ENTITIES_VIEW_PATH)
	ClientResponse<Rows> getNumberOfEntities();

	/**
	 * Retrieve all the Tuples belonging to CouchDBEntity with the table name equals to the given one.
	 *
	 * @param tableName
	 *            of the CouchDBEntity
	 * @return the ClientResponse with the {@link EntityTupleRows}
	 */
	@GET
	@Path(TuplesDesignDocument.ENTITY_TUPLE_BY_TABLE_NAME_PATH)
	ClientResponse<EntityTupleRows> getEntityTuplesByTableName(@QueryParam("value") String tableName);

}
