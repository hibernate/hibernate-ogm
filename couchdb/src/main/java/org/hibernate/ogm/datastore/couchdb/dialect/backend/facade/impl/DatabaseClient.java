/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.backend.facade.impl;

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

import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.designdocument.impl.DesignDocument;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.designdocument.impl.TuplesDesignDocument;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.Document;

/**
 * The Interface used by RESTEasy to create the REST calls used to interact with the CouchDB database instance. The
 * methods return a {@link Response} instance in order to have more information in case of failures.
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
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
	 * Retrieve the {@link org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.EntityDocument} with the given id
	 *
	 * @param id of the CouchDBEntity to retrieve
	 * @return the {@link Response} with the searched CouchDBEntity
	 */
	@GET
	@Path("{id}")
	Response getEntityById(@PathParam("id") String id);

	/**
	 * Retrieve the {@link org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.AssociationDocument} with the given id
	 *
	 * @param id of the CouchDBAssociation to retrieve
	 * @return the Response with the searched CouchDBAssociation
	 */
	@GET
	@Path("{id}")
	Response getAssociationById(@PathParam("id") String id);

	/**
	 * Retrieve the {@link org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.SequenceDocument} with the given id
	 *
	 * @param id of the CouchDBKeyValue to retrieve
	 * @return the {@link Response} with the searched CouchDBKeyValue
	 */
	@GET
	@Path("{id}")
	Response getKeyValueById(@PathParam("id") String id);

	/**
	 * Retrieve all the entity tuples with the table name equals to the given one.
	 *
	 * @param tableName name of the entity
	 * @return the {@link Response} with the
	 * {@link org.hibernate.ogm.datastore.couchdb.dialect.backend.json.designdocument.impl.EntityTupleRows}
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

	/**
	 * Retrieves the current revision of the design document with the given id.
	 *
	 * @param id the id of the design document of which to get the current revision
	 * @return the current revision of the specified design document, contained in the response's ETag. If the specified
	 * design document doesn't exist, the response's status will be 404.
	 */
	@HEAD
	@Path("_design/{id}")
	Response getCurrentRevisionOfDesignDocument(@PathParam("id") String id);
}
