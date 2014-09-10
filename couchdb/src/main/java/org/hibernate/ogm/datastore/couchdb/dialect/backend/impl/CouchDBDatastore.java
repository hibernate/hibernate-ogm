/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.backend.impl;

import java.util.List;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;

import org.hibernate.ogm.datastore.couchdb.dialect.backend.facade.impl.DatabaseClient;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.facade.impl.ServerClient;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.designdocument.impl.DesignDocument;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.designdocument.impl.EntityTupleRows;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.AssociationDocument;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.Document;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.EntityDocument;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.GenericResponse;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.SequenceDocument;
import org.hibernate.ogm.datastore.couchdb.logging.impl.Log;
import org.hibernate.ogm.datastore.couchdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.couchdb.util.impl.DatabaseIdentifier;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.spi.Tuple;
import org.jboss.resteasy.client.exception.ResteasyClientException;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * Provides access and interaction with a database instance of CouchDB.
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 * @author Gunnar Morling
 */
public class CouchDBDatastore {

	/**
	 * Size of the client connection pool used by the RestEasy HTTP client
	 */
	private static final int CONNECTION_POOL_SIZE = 10;

	private static final Log logger = LoggerFactory.getLogger();

	private final DatabaseIdentifier database;

	/**
	 * Client for accessing the server
	 */
	private final ResteasyClient client;

	/**
	 * Proxy providing safe access to the database functionality
	 */
	private final DatabaseClient databaseClient;

	private CouchDBDatastore(DatabaseIdentifier database) {
		logger.connectingToCouchDB( database.getDatabaseName() + "@" + database.getServerUri().toString() );

		client = createRestClient( database );
		databaseClient = client.target( database.getDatabaseUri() ).proxy( DatabaseClient.class );

		this.database = database;
	}

	/**
	 * Creates an instance of CouchDBDatastore.
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
		// create database if required
		ServerClient serverClient = client.target( database.getServerUri() ).proxy( ServerClient.class );

		if ( createDatabase ) {
			createDatabase( serverClient );
		}
		else if ( !databaseExists( serverClient, database.getDatabaseName() ) ) {
			throw logger.databaseDoesNotExistException( database.getDatabaseName() );
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

	public long nextValue(IdSourceKey key, int increment, int initialValue) {
		long value;
		try {
			SequenceDocument sequence = getSequence( key, initialValue );
			value = sequence.getValue();
			incrementValueAndSave( increment, sequence );
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
		if ( client != null ) {
			client.close();
		}
	}

	/**
	 * Returns a handle to the underlying CouchDB instance and database
	 * @return a handle to the underlying CouchDB instance and database
	 */
	public DatabaseIdentifier getDatabaseIdentifier() {
		return database;
	}

	private void createDatabase(ServerClient serverClient) {
		Response response = null;
		try {
			if ( !databaseExists( serverClient, database.getDatabaseName() ) ) {
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

	private boolean databaseExists(ServerClient serverClient, String databaseName) {
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

	private void incrementValueAndSave(int increment, SequenceDocument identifier) {
		identifier.increase( increment );
		saveDocument( identifier );
	}

	private String getTableName(EntityKeyMetadata entityKeyMetadata) {
		return entityKeyMetadata.getTable();
	}

	private static ResteasyClient createRestClient(DatabaseIdentifier database) {
		ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder();

		if ( database.getUserName() != null ) {
			clientBuilder.register( new BasicAuthentication( database.getUserName(), database.getPassword() ) );
		}

		// using a connection pool size > 1 causes a thread-safe pool implementation to be used under the hoods
		return clientBuilder
				.connectionPoolSize( CONNECTION_POOL_SIZE )
				.build();
	}

	private void updateDocumentRevision(Document document, String revision) {
		document.setRevision( revision );
	}

	private String createId(IdSourceKey key) {
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

	private SequenceDocument getSequence(IdSourceKey key, int initialValue) {
		Response response = null;
		try {
			String id = createId( key );
			response = databaseClient.getKeyValueById( id );
			if ( response.getStatus() == Response.Status.OK.getStatusCode() ) {
				return response.readEntity( SequenceDocument.class );
			}
			else if ( response.getStatus() == Response.Status.NOT_FOUND.getStatusCode() ) {
				SequenceDocument identifier = new SequenceDocument( key.getMetadata().getValueColumnName(), initialValue );
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
