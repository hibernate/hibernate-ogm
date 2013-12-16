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
package org.hibernate.ogm.dialect.couchdb.impl.backend.facade;

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

import org.hibernate.ogm.dialect.couchdb.impl.backend.json.Document;
import org.hibernate.ogm.dialect.couchdb.impl.backend.json.designdocument.AssociationsDesignDocument;
import org.hibernate.ogm.dialect.couchdb.impl.backend.json.designdocument.DesignDocument;
import org.hibernate.ogm.dialect.couchdb.impl.backend.json.designdocument.EntitiesDesignDocument;
import org.hibernate.ogm.dialect.couchdb.impl.backend.json.designdocument.TuplesDesignDocument;

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
	 * Save a {@link Document} with the given id
	 *
	 * @param document to be saved
	 * @param id to use for the document
	 * @return the {@link Response} from CouchDB
	 */
	@PUT
	@Path("{id}")
	Response saveDocument(Document document, @PathParam("id") String id);

	/**
	 * Save a {@link DesignDocument} with the given id
	 *
	 * @param design to be saved
	 * @param id to use for the design
	 * @return the {@link Response} from CouchDB
	 */
	@PUT
	@Path("_design/{id}")
	Response saveDesign(DesignDocument design, @PathParam("id") String id);

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
	 * Retrieve the {@link org.hibernate.ogm.dialect.couchdb.impl.backend.json.EntityDocument} with the given id
	 *
	 * @param id of the CouchDBEntity to retrieve
	 * @return the {@link Response} with the searched CouchDBEntity
	 */
	@GET
	@Path("{id}")
	Response getEntityById(@PathParam("id") String id);

	/**
	 * Retrieve the {@link org.hibernate.ogm.dialect.couchdb.impl.backend.json.AssociationDocument} with the given id
	 *
	 * @param id of the CouchDBAssociation to retrieve
	 * @return the Response with the searched CouchDBAssociation
	 */
	@GET
	@Path("{id}")
	Response getAssociationById(@PathParam("id") String id);

	/**
	 * Retrieve the {@link org.hibernate.ogm.dialect.couchdb.impl.backend.json.SequenceDocument} with the given id
	 *
	 * @param id of the CouchDBKeyValue to retrieve
	 * @return the {@link Response} with the searched CouchDBKeyValue
	 */
	@GET
	@Path("{id}")
	Response getKeyValueById(@PathParam("id") String id);

	/**
	 * Retrieve the number of associations stored in the database.
	 *
	 * @return the response in form of a {@link org.hibernate.ogm.dialect.couchdb.impl.backend.json.AssociationCountResponse}
	 * entity
	 */
	@GET
	// TODO Can the parameter be avoided? It is always set to true, but adding "?group=true" to the path itself causes
	// the question mark to be URL-encoded
	@Path(AssociationsDesignDocument.ASSOCIATION_COUNT_PATH)
	Response getNumberOfAssociations(@QueryParam("group") boolean group);

	/**
	 * Retrieve the number of entities stored in the database
	 *
	 * @return the Response CouchDB with the {@link org.hibernate.ogm.dialect.couchdb.impl.backend.json.designdocument.Rows}
	 */
	@GET
	@Path(EntitiesDesignDocument.ENTITY_COUNT_PATH)
	Response getNumberOfEntities();

	/**
	 * Retrieve all the entity tuples with the table name equals to the given one.
	 *
	 * @param tableName name of the entity
	 * @return the {@link Response} with the
	 * {@link org.hibernate.ogm.dialect.couchdb.impl.backend.json.designdocument.EntityTupleRows}
	 */
	@GET
	@Path(TuplesDesignDocument.ENTITY_TUPLE_BY_TABLE_NAME_PATH)
	Response getEntityTuplesByTableName(@QueryParam("key") String tableName);

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
