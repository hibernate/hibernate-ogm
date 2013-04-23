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
package org.hibernate.ogm.datastore.couchdb.impl;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.hibernate.HibernateException;
import org.hibernate.ogm.dialect.couchdb.designdocument.AssociationsDesignDocument;
import org.hibernate.ogm.dialect.couchdb.designdocument.EntitiesDesignDocument;
import org.hibernate.ogm.dialect.couchdb.designdocument.EntityTupleRows;
import org.hibernate.ogm.dialect.couchdb.designdocument.Rows;
import org.hibernate.ogm.dialect.couchdb.designdocument.TuplesDesignDocument;
import org.hibernate.ogm.dialect.couchdb.model.CouchDBTuple;
import org.hibernate.ogm.dialect.couchdb.resteasy.CouchDBAssociation;
import org.hibernate.ogm.dialect.couchdb.resteasy.CouchDBDocument;
import org.hibernate.ogm.dialect.couchdb.resteasy.CouchDBEntity;
import org.hibernate.ogm.dialect.couchdb.resteasy.CouchDBKeyValue;
import org.hibernate.ogm.dialect.couchdb.resteasy.CouchDBResponse;
import org.hibernate.ogm.dialect.couchdb.resteasy.DatabaseClient;
import org.hibernate.ogm.dialect.couchdb.resteasy.ServerClient;
import org.hibernate.ogm.dialect.couchdb.util.DataBaseURL;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.logging.couchdb.impl.Log;
import org.hibernate.ogm.logging.couchdb.impl.LoggerFactory;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.client.core.executors.ApacheHttpClientExecutor;
import org.jboss.resteasy.client.exception.ResteasyClientException;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.persistence.OptimisticLockException;
import java.util.List;

/**
 * Provides access and interaction with a database instance of CouchDB.
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
public class CouchDBDatastore {

	private static final Log logger = LoggerFactory.getLogger();
	private DatabaseClient databaseClient;
	private ServerClient serverClient;
	private ClientExecutor clientExecutor;

	private CouchDBDatastore(DataBaseURL databaseUrl, String userName, String password) {
		logger.connectingToCouchDB( databaseUrl.toString() );
		initClientExecutor( userName, password );
		serverClient = createServerClient( databaseUrl );
		databaseClient = createDataBaseClient( databaseUrl );
	}

	/**
	 * Creates an instance of CouchDBDatastore, check if the CouchDB Design Document
	 * necessary to retrieve some data (e.g. number of associations and entities) are present and create them if not
	 *
	 * @param databaseURL
	 *            the url of the database
	 * @param userName
	 *            the username of the database user or null if authentication is not required
	 * @param password
	 *            the password of the database user or null if authentication is not required
	 * @param createDatabase
	 *            if true the database is created
	 * @return an instance of CouchDBDatastore
	 */
	public static CouchDBDatastore newInstance(DataBaseURL databaseURL, String userName, String password,
			boolean createDatabase) {
		RegisterBuiltin.register( ResteasyProviderFactory.getInstance() );

		CouchDBDatastore couchDBDatastore = new CouchDBDatastore( databaseURL, userName, password );

		if ( createDatabase ) {
			couchDBDatastore.createDatabase( databaseURL );
		}

		couchDBDatastore.createDesignDocumentsIfNotExist();

		return couchDBDatastore;
	}

	/**
	 * Saves a Document to the database
	 *
	 * @param document
	 *            the {@link CouchDBDocument} to be saved
	 * @return the saved CouchDBDocument
	 */
	public CouchDBDocument saveDocument(CouchDBDocument document) {
		ClientResponse<CouchDBResponse> response = null;
		try {
			response = databaseClient.saveDocument( document, document.getId() );
			if ( response.getStatus() == HttpStatus.SC_CREATED ) {
				updateDocumentRevision( document, response.getEntity().getRev() );
			}
			else if ( response.getStatus() == HttpStatus.SC_CONFLICT ) {
				throw new OptimisticLockException();
			}
			else {
				throw logger.errorCreatingDocument( response.getStatus(), response.getEntity().getError(), response
						.getEntity().getReason() );
			}
			return document;
		}
		catch ( ResteasyClientException e ) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.releaseConnection();
			}
		}
	}

	/**
	 * Retrieves a {@link CouchDBEntity} from the database
	 *
	 * @param id
	 *            the id of the CouchDBEntity to retrieve
	 * @return the found CouchDBEntity or null
	 */
	public CouchDBEntity getEntity(String id) {
		ClientResponse<CouchDBEntity> response = null;
		try {
			response = databaseClient.getEntityId( id );
			if ( response.getStatus() == HttpStatus.SC_NOT_FOUND ) {
				return null;
			}
			else if ( response.getStatus() == HttpStatus.SC_OK ) {
				return response.getEntity();
			}
			else {
				throw logger.errorRetrievingEntity( id, response.getStatus() );
			}
		}
		catch ( ResteasyClientException e ) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.releaseConnection();
			}
		}
	}

	/**
	 * Retrieves a {@link CouchDBAssociation} from the database
	 *
	 * @param id
	 *            the id of the CouchDBAssociation to retrieve
	 * @return the found CouchDBAssociation or null
	 */
	public CouchDBAssociation getAssociation(String id) {
		ClientResponse<CouchDBAssociation> response = null;
		try {
			response = databaseClient.getAssociationById( id );
			if ( response.getStatus() == HttpStatus.SC_NOT_FOUND ) {
				return null;
			}
			else if ( response.getStatus() == HttpStatus.SC_OK ) {
				return response.getEntity();
			}
			else {
				throw logger.errorRetrievingAssociation( id, response.getStatus() );
			}
		}
		catch ( ResteasyClientException e ) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.releaseConnection();
			}
		}
	}

	/**
	 * Retrieves all the tuples matching the {@link EntityKeyMetadata}
	 *
	 * @param entityKeyMetadata
	 *            the EntityKeyMetadata used to filter the tuples
	 * @return all the {@link CouchDBTuple} matching the given entityKeyMetadata
	 */
	public List<CouchDBTuple> getTuples(EntityKeyMetadata entityKeyMetadata) {
		final String tableName = getTableName( entityKeyMetadata );
		return getTuplesByTableName( tableName );
	}

	public long nextValue(RowKey key, int increment, int initialValue) {
		long value;
		try {
			CouchDBKeyValue identifier = getNextKeyValue( createId( key ), initialValue );
			value = identifier.getValue();
			saveIntegralIncreasedValue( increment, identifier );
		}
		catch ( ClientResponseFailure crf ) {
			throw logger.errorCalculatingNextValue( crf );
		}
		return value;
	}

	/**
	 * Deletes a Document from the database
	 *
	 * @param toDelete
	 *            the CouchDBDocument to be deleted
	 */
	public void deleteDocument(CouchDBDocument toDelete) {
		ClientResponse<CouchDBResponse> response = null;
		try {
			response = databaseClient.deleteDocument( toDelete.getId(), toDelete.getRevision() );
			if ( response.getStatus() == HttpStatus.SC_CONFLICT ) {
				response.releaseConnection();
				throw new OptimisticLockException();
			}
			else if ( response.getStatus() != HttpStatus.SC_OK ) {
				throw logger.errorDeletingDocument( response.getStatus(), response.getEntity().getError(), response
						.getEntity().getReason() );
			}
		}
		catch ( ResteasyClientException e ) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.releaseConnection();
			}
		}
	}

	/**
	 * Retrieves the number of CouchDBAssociation stored in the database
	 *
	 * @return the number of CouchDBAssociation stored in the database
	 */
	public int getNumberOfAssociations() {
		ClientResponse<Rows> response = null;
		try {
			response = databaseClient.getNumberOfAssociations();
			if ( response.getStatus() == HttpStatus.SC_OK ) {
				return response.getEntity().size();
			}
			else {
				throw logger.unableToRetrieveTheNumberOfAssociations( response.getStatus() );
			}
		}
		catch ( ResteasyClientException e ) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.releaseConnection();
			}
		}
	}

	/**
	 * Retrieves the number of CouchDBEntity stored in the database
	 *
	 * @return the number of CouchDBEntity stored in the database
	 */
	public int getNumberOfEntities() {
		ClientResponse<Rows> response = null;
		try {
			response = databaseClient.getNumberOfEntities();
			if ( response.getStatus() == HttpStatus.SC_OK ) {
				return response.getEntity().size();
			}
			else {
				throw logger.unableToRetrieveTheNumberOfEntities( response.getStatus() );
			}
		}
		catch ( ResteasyClientException e ) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.releaseConnection();
			}
		}
	}

	/**
	 * Deletes the database
	 */
	public void dropDatabase() {
		ClientResponse response = null;
		try {
			response = databaseClient.dropDatabase();
			if ( response.getStatus() != HttpStatus.SC_OK ) {
				response.releaseConnection();
				throw logger.errorDroppingDatabase( response.getStatus() );
			}
		}
		catch ( ResteasyClientException e ) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.releaseConnection();
			}
		}
	}

	/**
	 * Releases all the resources
	 */
	public void shutDown() {
		try {
			clientExecutor.close();
		}
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	private void createDatabase(DataBaseURL url) {
		ClientResponse<CouchDBResponse> response = null;
		try {
			if ( !databaseExists( url.getDataBaseName() ) ) {
				response = serverClient.createDatabase( url.getDataBaseName() );
				if ( response.getStatus() != HttpStatus.SC_CREATED ) {
					throw logger.errorCreatingDatabase( url.getDataBaseName(), response.getStatus(), response
							.getEntity().getError(), response.getEntity().getReason() );
				}
			}
		}
		catch ( ResteasyClientException e ) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.releaseConnection();
			}
		}
	}

	private boolean databaseExists(String databaseName) {
		ClientResponse<List<String>> response = null;
		try {
			response = serverClient.getAllDatabaseNames();
			if ( response.getStatus() == HttpStatus.SC_OK ) {
				if ( response.getEntity().contains( databaseName ) ) {
					return true;
				}
				return false;
			}
			else {
				throw logger.unableToRetrieveTheListOfDatabase( response.getStatus() );
			}
		}
		catch ( ResteasyClientException e ) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.releaseConnection();
			}
		}
	}

	private List<CouchDBTuple> getTuplesByTableName(String tableName) {
		ClientResponse<EntityTupleRows> response = null;
		try {
			response = databaseClient.getEntityTuplesByTableName( tableName );
			if ( response.getStatus() == HttpStatus.SC_OK ) {
				return response.getEntity().getTuples();
			}
			else {
				throw logger.unableToRetrieveTheTupleByEntityKeyMetadata( tableName, response.getStatus() );
			}
		}
		catch ( ResteasyClientException e ) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.releaseConnection();
			}
		}
	}

	private void saveIntegralIncreasedValue(int increment, CouchDBKeyValue identifier) {
		identifier.increase( increment );
		saveDocument( identifier );
	}

	private String getTableName(EntityKeyMetadata entityKeyMetadata) {
		return entityKeyMetadata.getTable();
	}

	private ServerClient createServerClient(DataBaseURL databaseUrl) {
		if ( clientExecutor == null ) {
			return ProxyFactory.create( ServerClient.class, databaseUrl.getServerUrl() );
		}
		else {
			return ProxyFactory.create( ServerClient.class, databaseUrl.getServerUrl(), clientExecutor );
		}
	}

	private DatabaseClient createDataBaseClient(DataBaseURL databaseUrl) {
		if ( clientExecutor == null ) {
			return ProxyFactory.create( DatabaseClient.class, databaseUrl.toString() );
		}
		else {
			return ProxyFactory.create( DatabaseClient.class, databaseUrl.toString(), clientExecutor );
		}
	}

	private void initClientExecutor(String userName, String password) {
		HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
		HttpClient httpClient = new HttpClient( connectionManager );
		if ( userName != null ) {
			Credentials credentials = new UsernamePasswordCredentials( userName, password );
			httpClient.getState().setCredentials( AuthScope.ANY, credentials );
			httpClient.getParams().setAuthenticationPreemptive( true );
		}
		clientExecutor = new ApacheHttpClientExecutor( httpClient );
	}

	private void updateDocumentRevision(CouchDBDocument document, String revision) {
		document.setRevision( revision );
	}

	private String createId(RowKey key) {
		String id = key.getTable();
		for ( int i = 0; i < key.getColumnNames().length; i++ ) {
			id += key.getColumnNames()[i];
		}
		for ( int i = 0; i < key.getColumnValues().length; i++ ) {
			id += key.getColumnValues()[i];
		}
		return id;
	}

	private CouchDBKeyValue getNextKeyValue(String id, int initialValue) {
		ClientResponse<CouchDBKeyValue> response = null;
		try {
			response = databaseClient.getKeyValue( id );
			if ( response.getStatus() == HttpStatus.SC_OK ) {
				return response.getEntity();
			}
			else if ( response.getStatus() == HttpStatus.SC_NOT_FOUND ) {
				CouchDBKeyValue identifier = new CouchDBKeyValue( initialValue );
				identifier.setId( id );
				return identifier;
			}
			else {
				throw logger.errorRetrievingIntegral( response.getStatus() );
			}
		}
		catch ( ResteasyClientException e ) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.releaseConnection();
			}
		}
	}

	private void createDesignDocumentsIfNotExist() {
		if ( !designDocumentsExist() ) {
			createDesignDocuments();
		}
	}

	private void createDesignDocuments() {
		saveDocument( new EntitiesDesignDocument() );
		saveDocument( new AssociationsDesignDocument() );
		saveDocument( new TuplesDesignDocument() );
	}

	private boolean designDocumentsExist() {
		try {
			getNumberOfEntities();
			getNumberOfAssociations();
		}
		catch ( HibernateException e ) {
			return false;
		}
		return true;
	}

}
