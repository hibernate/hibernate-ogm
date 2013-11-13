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
package org.hibernate.ogm.dialect.couchdb.json;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.hibernate.ogm.dialect.couchdb.designdocument.AssociationsDesignDocument;
import org.hibernate.ogm.dialect.couchdb.designdocument.CouchDBDesignDocument;
import org.hibernate.ogm.dialect.couchdb.designdocument.EntitiesDesignDocument;
import org.hibernate.ogm.dialect.couchdb.designdocument.TuplesDesignDocument;

/**
 * The Interface used by RESTEasy to create the REST calls used to interact with the CouchDB database instance. The
 * methods return a {@link Response} instance in order to have more information in case of failures.
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
	 * @param document to be saved
	 * @param id to use for the document
	 * @return the {@link Response} from CouchDB
	 */
	@PUT
	@Path("{id}")
	Response saveDocument(CouchDBDocument document, @PathParam("id") String id);

	/**
	 * Save a {@link CouchDBDesignDocument} with the given id
	 *
	 * @param design to be saved
	 * @param id to use for the design
	 * @return the {@link Response} from CouchDB
	 */
	@PUT
	@Path("_design/{id}")
	Response saveDesign(CouchDBDesignDocument design, @PathParam("id") String id);

	/**
	 * Delete the CouchDBDocument with the given id and revision
	 *
	 * @param id of the CouchDBDocument to delete
	 * @param revision of the CouchDBDocument to delete
	 * @return the Response
	 */
	@DELETE
	@Path("{id}")
	Response deleteDocument(@PathParam("id") String id, @QueryParam("rev") String revision);

	/**
	 * Delete the Database from CouchDB
	 *
	 * @return the Response
	 */
	@DELETE
	@Path("/")
	Response dropDatabase();

	/**
	 * Retrieve the {@link CouchDBEntity} with the given id
	 *
	 * @param id of the CouchDBEntity to retrieve
	 * @return the {@link Response} with the searched CouchDBEntity
	 */
	@GET
	@Path("{id}")
	Response getEntityById(@PathParam("id") String id);

	/**
	 * Retrieve the {@link CouchDBAssociation} with the given id
	 *
	 * @param id of the CouchDBAssociation to retrieve
	 * @return the Response with the searched CouchDBAssociation
	 */
	@GET
	@Path("{id}")
	Response getAssociationById(@PathParam("id") String id);

	/**
	 * Retrieve the {@link CouchDBKeyValue} with the given id
	 *
	 * @param id of the CouchDBKeyValue to retrieve
	 * @return the {@link Response} with the searched CouchDBKeyValue
	 */
	@GET
	@Path("{id}")
	Response getKeyValueById(@PathParam("id") String id);

	/**
	 * Retrieve the number of {@link CouchDBAssociation} stored in the database
	 *
	 * @return the Response with the searched {@link CouchDBKeyValue}
	 */
	@GET
	@Path(AssociationsDesignDocument.NUMBER_OF_ASSOCIATIONS_VIEW_PATH)
	Response getNumberOfAssociations();

	/**
	 * Retrieve the number of entities stored in the database
	 *
	 * @return the Response CouchDB with the {@link org.hibernate.ogm.dialect.couchdb.designdocument.Rows}
	 */
	@GET
	@Path(EntitiesDesignDocument.NUMBER_OF_ENTITIES_VIEW_PATH)
	Response getNumberOfEntities();

	/**
	 * Retrieve all the entity tuples with the table name equals to the given one.
	 *
	 * @param tableName name of the entity
	 * @return the {@link Response} with the {@link org.hibernate.ogm.dialect.couchdb.designdocument.EntityTupleRows}
	 */
	@GET
	@Path(TuplesDesignDocument.ENTITY_TUPLE_BY_TABLE_NAME_PATH)
	Response getEntityTuplesByTableName(@QueryParam("value") String tableName);

	/**
	 * Retrieves the current revision of the document with the given id.
	 *
	 * @param id the id of the document of which to get the current revision
	 * @return the current revision of the specified document, contained in the response's ETag. If the specified
	 * document doesn't exist, the response's status will be 404.
	 */
	@HEAD
	@Path("{id}")
	Response getCurrentRevision(@PathParam("id") String id);
}
