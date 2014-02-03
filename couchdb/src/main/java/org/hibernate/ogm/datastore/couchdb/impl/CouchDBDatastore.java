/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.couchdb.impl;

import java.util.List;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;

import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.couchdb.impl.backend.facade.DatabaseClient;
import org.hibernate.ogm.dialect.couchdb.impl.backend.facade.ServerClient;
import org.hibernate.ogm.dialect.couchdb.impl.backend.json.AssociationDocument;
import org.hibernate.ogm.dialect.couchdb.impl.backend.json.Document;
import org.hibernate.ogm.dialect.couchdb.impl.backend.json.EntityDocument;
import org.hibernate.ogm.dialect.couchdb.impl.backend.json.GenericResponse;
import org.hibernate.ogm.dialect.couchdb.impl.backend.json.SequenceDocument;
import org.hibernate.ogm.dialect.couchdb.impl.backend.json.designdocument.DesignDocument;
import org.hibernate.ogm.dialect.couchdb.impl.backend.json.designdocument.EntityTupleRows;
import org.hibernate.ogm.dialect.couchdb.impl.backend.json.designdocument.TuplesDesignDocument;
import org.hibernate.ogm.dialect.couchdb.impl.util.DatabaseIdentifier;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.logging.couchdb.impl.Log;
import org.hibernate.ogm.logging.couchdb.impl.LoggerFactory;
import org.jboss.resteasy.client.exception.ResteasyClientException;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * Provides access and interaction with a database instance of CouchDB.
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 * @author Gunnar Morling
 */
public class CouchDBDatastore {

	private static final Log logger = LoggerFactory.getLogger();
	private final DatabaseClient databaseClient;
	private final ServerClient serverClient;
	private final DatabaseIdentifier database;

	private CouchDBDatastore(DatabaseIdentifier database) {
		logger.connectingToCouchDB( database.getDatabaseName() + "@" + database.getServerUri().toString() );
		serverClient = createServerClient( database );
		databaseClient = createDataBaseClient( database );
		this.database = database;
	}

	/**
	 * Creates an instance of CouchDBDatastore, check if the CouchDB Design Document necessary to retrieve some data
	 * (e.g. number of associations and entities) are present and create them if not
	 *
	 * @param database a handle to the database
	 * @param createDatabase if true the database is created
	 * @return an instance of CouchDBDatastore
	 */
	public static CouchDBDatastore newInstance(DatabaseIdentifier database, boolean createDatabase) {
		RegisterBuiltin.register( ResteasyProviderFactory.getInstance() );

		CouchDBDatastore couchDBDatastore = new CouchDBDatastore( database );
		couchDBDatastore.initialize( createDatabase );

		return couchDBDatastore;
	}

	private void initialize(boolean createDatabase) {
		if ( createDatabase ) {
			createDatabase();
		}
		else if ( !databaseExists( database.getDatabaseName() ) ) {
			throw logger.databaseDoesNotExistException( database.getDatabaseName() );
		}

		if ( !exists( TuplesDesignDocument.DOCUMENT_ID, true ) ) {
			saveDocument( new TuplesDesignDocument() );
		}
	}

	/**
	 * Saves a Document to the database
	 *
	 * @param document the {@link Document} to be saved
	 * @return the saved CouchDBDocument
	 */
	public Document saveDocument(Document document) {
		return doSaveDocument( document, false );
	}

	/**
	 * Saves the given design document in the database.
	 *
	 * @param design the design document to save
	 * @return the saved document
	 */
	public Document saveDocument(DesignDocument design) {
		return doSaveDocument( design, true );
	}

	private Document doSaveDocument(Document document, boolean isDesignDocument) {
		Response response = null;
		try {
			if ( isDesignDocument ) {
				response = databaseClient.saveDesign( (DesignDocument) document, document.getId() );
			}
			else {
				response = databaseClient.saveDocument( document, document.getId() );
			}
			if ( response.getStatus() == Response.Status.CREATED.getStatusCode() ) {
				GenericResponse entity = response.readEntity( GenericResponse.class );
				updateDocumentRevision( document, entity.getRev() );
			}
			else if ( response.getStatus() == Response.Status.CONFLICT.getStatusCode() ) {
				throw logger.getDocumentHasBeenConcurrentlyModifiedException( document.getId() );
			}
			else {
				GenericResponse entity = response.readEntity( GenericResponse.class );
				throw logger.errorCreatingDocument( response.getStatus(), entity.getError(), entity.getReason() );
			}
			return document;
		}
		catch (ResteasyClientException e) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.close();
			}
		}
	}

	/**
	 * Retrieves a {@link EntityDocument} from the database
	 *
	 * @param id the id of the CouchDBEntity to retrieve
	 * @return the found CouchDBEntity or null
	 */
	public EntityDocument getEntity(String id) {
		Response response = null;
		try {
			response = databaseClient.getEntityById( id );
			if ( response.getStatus() == Response.Status.NOT_FOUND.getStatusCode() ) {
				return null;
			}
			else if ( response.getStatus() == Response.Status.OK.getStatusCode() ) {
				return response.readEntity( EntityDocument.class );
			}
			else {
				GenericResponse responseEntity = response.readEntity( GenericResponse.class );
				throw logger.errorRetrievingEntity( id, response.getStatus(), responseEntity.getError(), responseEntity.getReason() );
			}
		}
		catch (ResteasyClientException e) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.close();
			}
		}
	}

	/**
	 * Returns the current revision of the document with the given id.
	 *
	 * @param documentId the document's id
	 * @param isDesignDocument whether the given id identifies a design document or not
	 * @return the current revision of the specified document or {@code null} if no document with the given id exists
	 */
	public String getCurrentRevision(String documentId, boolean isDesignDocument) {
		Response response = null;

		try {
			response = isDesignDocument ? databaseClient.getCurrentRevisionOfDesignDocument( documentId ) : databaseClient.getCurrentRevision( documentId );

			if ( response.getStatus() == Response.Status.OK.getStatusCode() ) {
				//The revision is returned as ETag for HEAD requests
				return response.getEntityTag().getValue();
			}
			else if ( response.getStatus() == Response.Status.NOT_FOUND.getStatusCode() ) {
				return null;
			}
			else {
				GenericResponse responseEntity = response.hasEntity() ? response.readEntity( GenericResponse.class ) : null;

				throw logger.errorRetrievingCurrentRevision(
						documentId,
						response.getStatus(),
						responseEntity != null ? responseEntity.getError() : null,
						responseEntity != null ? responseEntity.getReason() : null
				);
			}
		}
		catch (ResteasyClientException e) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.close();
			}
		}
	}

	/**
	 * Whether the given document exists in the datastore or not.
	 */
	public boolean exists(String documentId, boolean isDesignDocument) {
		return getCurrentRevision( documentId, isDesignDocument ) != null;
	}

	/**
	 * Retrieves a {@link AssociationDocument} from the database
	 *
	 * @param id the id of the CouchDBAssociation to retrieve
	 * @return the found CouchDBAssociation or null
	 */
	public AssociationDocument getAssociation(String id) {
		Response response = null;
		try {
			response = databaseClient.getAssociationById( id );
			if ( response.getStatus() == Response.Status.NOT_FOUND.getStatusCode() ) {
				return null;
			}
			else if ( response.getStatus() == Response.Status.OK.getStatusCode() ) {
				return response.readEntity( AssociationDocument.class );
			}
			else {
				GenericResponse responseEntity = response.readEntity( GenericResponse.class );
				throw logger.errorRetrievingAssociation( id, response.getStatus(), responseEntity.getError(), responseEntity.getReason() );
			}
		}
		catch (ResteasyClientException e) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.close();
			}
		}
	}

	/**
	 * Retrieves all the tuples matching the {@link EntityKeyMetadata}
	 *
	 * @param entityKeyMetadata the EntityKeyMetadata used to filter the tuples
	 * @return all the tuples matching the given entityKeyMetadata
	 */
	public List<Tuple> getTuples(EntityKeyMetadata entityKeyMetadata) {
		final String tableName = getTableName( entityKeyMetadata );
		return getTuplesByTableName( tableName );
	}

	public long nextValue(RowKey key, int increment, int initialValue) {
		long value;
		try {
			SequenceDocument identifier = getNextKeyValue( createId( key ), initialValue );
			value = identifier.getValue();
			saveIntegralIncreasedValue( increment, identifier );
		}
		catch (ResteasyClientException crf) {
			throw logger.errorCalculatingNextValue( crf );
		}
		return value;
	}

	/**
	 * Deletes a Document from the database
	 *
	 * @param id the id of the document to be deleted
	 * @param revision the revision of the document to be deleted
	 */
	public void deleteDocument(String id, String revision) {
		Response response = null;
		try {
			response = databaseClient.deleteDocument( id, revision );
			if ( response.getStatus() == Response.Status.CONFLICT.getStatusCode() ) {
				throw logger.getDocumentHasBeenConcurrentlyModifiedException( id );
			}
			else if ( response.getStatus() != Response.Status.OK.getStatusCode() &&  response.getStatus() != Response.Status.NOT_FOUND.getStatusCode() ) {
				throw logger.errorDeletingDocument( response.getStatus(), null, null );
			}
		}
		catch (ResteasyClientException e) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.close();
			}
		}
	}

	/**
	 * Deletes the database
	 */
	public void dropDatabase() {
		Response response = null;
		try {
			response = databaseClient.dropDatabase();
			if ( response.getStatus() != Response.Status.OK.getStatusCode() ) {
				GenericResponse responseEntity = response.readEntity( GenericResponse.class );
				throw logger.errorDroppingDatabase( response.getStatus(), responseEntity.getError(), responseEntity.getReason() );
			}
		}
		catch (ResteasyClientException e) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.close();
			}
		}
	}

	/**
	 * Releases all the resources
	 */
	public void shutDown() {
	}

	/**
	 * Returns a handle to the underlying CouchDB instance and database
	 * @return a handle to the underlying CouchDB instance and database
	 */
	public DatabaseIdentifier getDatabaseIdentifier() {
		return database;
	}

	private void createDatabase() {
		Response response = null;
		try {
			if ( !databaseExists( database.getDatabaseName() ) ) {
				response = serverClient.createDatabase( database.getDatabaseName() );
				if ( response.getStatus() != Response.Status.CREATED.getStatusCode() ) {
					GenericResponse entity = response.readEntity( GenericResponse.class );
					throw logger.errorCreatingDatabase( database.getDatabaseName(), response.getStatus(), entity.getError(), entity.getReason() );
				}
			}
		}
		catch (ResteasyClientException e) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.close();
			}
		}
	}

	private boolean databaseExists(String databaseName) {
		Response response = null;
		try {
			response = serverClient.getAllDatabaseNames();
			if ( response.getStatus() == Response.Status.OK.getStatusCode() ) {
				@SuppressWarnings("unchecked")
				List<String> entity = response.readEntity( List.class );
				if ( entity.contains( databaseName ) ) {
					return true;
				}
				return false;
			}
			else {
				GenericResponse responseEntity = response.readEntity( GenericResponse.class );
				throw logger.unableToRetrieveTheListOfDatabase( response.getStatus(), responseEntity.getError(), responseEntity.getReason() );
			}
		}
		catch (ProcessingException e) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.close();
			}
		}
	}

	private List<Tuple> getTuplesByTableName(String tableName) {
		Response response = null;
		try {
			response = databaseClient.getEntityTuplesByTableName( "\"" + tableName + "\"" );
			if ( response.getStatus() == Response.Status.OK.getStatusCode() ) {
				return response.readEntity( EntityTupleRows.class ).getTuples();
			}
			else {
				GenericResponse responseEntity = response.readEntity( GenericResponse.class );
				throw logger.unableToRetrieveTheTupleByEntityKeyMetadata( tableName, response.getStatus(), responseEntity.getError(), responseEntity.getReason() );
			}
		}
		catch (ResteasyClientException e) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.close();
			}
		}
	}

	private void saveIntegralIncreasedValue(int increment, SequenceDocument identifier) {
		identifier.increase( increment );
		saveDocument( identifier );
	}

	private String getTableName(EntityKeyMetadata entityKeyMetadata) {
		return entityKeyMetadata.getTable();
	}

	private static ServerClient createServerClient(DatabaseIdentifier database) {
		ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder();

		if ( database.getUserName() != null ) {
			clientBuilder.register( new BasicAuthentication( database.getUserName(), database.getPassword() ) );
		}

		ResteasyClient client = clientBuilder.build();
		ResteasyWebTarget target = client.target( database.getServerUri() );

		return target.proxy( ServerClient.class );
	}

	private static DatabaseClient createDataBaseClient(DatabaseIdentifier database) {
		ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder();

		if ( database.getUserName() != null ) {
			clientBuilder.register( new BasicAuthentication( database.getUserName(), database.getPassword() ) );
		}

		ResteasyClient client = clientBuilder.build();
		ResteasyWebTarget target = client.target( database.getDatabaseUri() );

		return target.proxy( DatabaseClient.class );
	}

	private void updateDocumentRevision(Document document, String revision) {
		document.setRevision( revision );
	}

	private String createId(RowKey key) {
		StringBuilder builder = new StringBuilder( key.getTable() );
		builder.append( ":" );
		for ( int i = 0; i < key.getColumnNames().length; i++ ) {
			builder.append( key.getColumnNames()[i] );
		}
		builder.append( ":" );
		for ( int i = 0; i < key.getColumnValues().length; i++ ) {
			builder.append( key.getColumnValues()[i] );
		}
		return builder.toString();
	}

	private SequenceDocument getNextKeyValue(String id, int initialValue) {
		Response response = null;
		try {
			response = databaseClient.getKeyValueById( id );
			if ( response.getStatus() == Response.Status.OK.getStatusCode() ) {
				return response.readEntity( SequenceDocument.class );
			}
			else if ( response.getStatus() == Response.Status.NOT_FOUND.getStatusCode() ) {
				SequenceDocument identifier = new SequenceDocument( initialValue );
				identifier.setId( id );
				return identifier;
			}
			else {
				GenericResponse responseEntity = response.readEntity( GenericResponse.class );
				throw logger.errorRetrievingKeyValue( response.getStatus(), responseEntity.getError(), responseEntity.getReason() );
			}
		}
		catch (ResteasyClientException e) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.close();
			}
		}
	}
}
