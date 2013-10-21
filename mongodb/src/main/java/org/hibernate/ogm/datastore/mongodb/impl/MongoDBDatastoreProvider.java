/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012-2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.mongodb.impl;

import java.io.Closeable;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.ogm.datastore.mongodb.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.impl.configuration.MongoDBConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.mongodb.MassIndexingMongoDBTupleSnapshot;
import org.hibernate.ogm.dialect.mongodb.MongoDBDialect;
import org.hibernate.ogm.dialect.mongodb.query.parsing.MongoDBBasedQueryParserService;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.logging.mongodb.impl.Log;
import org.hibernate.ogm.logging.mongodb.impl.LoggerFactory;
import org.hibernate.ogm.options.mongodb.mapping.impl.MongoDBEntityOptions;
import org.hibernate.ogm.options.mongodb.mapping.impl.MongoDBGlobalOptions;
import org.hibernate.ogm.options.mongodb.mapping.impl.MongoDBPropertyOptions;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;
import org.hibernate.ogm.service.impl.QueryParserService;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

/**
 * Provides access to MongoDB system
 *
 * @author Guillaume Scheibel<guillaume.scheibel@gmail.com>
 */
public class MongoDBDatastoreProvider implements DatastoreProvider, Startable, Stoppable, Configurable {

	private static final Log log = LoggerFactory.getLogger();

	private boolean isCacheStarted;
	private MongoClient mongo;
	private DB mongoDb;
	private final MongoDBConfiguration config = new MongoDBConfiguration();

	@Override
	public void configure(Map configurationValues) {
		this.config.initialize( configurationValues );
	}

	public AssociationStorageType getAssociationStorage() {
		return config.getAssociationStorage();
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return MongoDBDialect.class;
	}

	@Override
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		return MongoDBBasedQueryParserService.class;
	}

	@Override
	public MongoDBGlobalOptions getConfigurationBuilder(ConfigurationContext context) {
		return context.createGlobalContext( MongoDBGlobalOptions.class, MongoDBEntityOptions.class, MongoDBPropertyOptions.class );
	}

	@Override
	public void start() {
		if ( !isCacheStarted ) {
			try {
				ServerAddress serverAddress = new ServerAddress( config.getHost(), config.getPort() );
				this.mongo = new MongoClient( serverAddress, config.buildOptions() );
				this.isCacheStarted = true;
			}
			catch ( UnknownHostException e ) {
				throw log.mongoOnUnknownHost( config.getHost() );
			}
			catch ( RuntimeException e ) {
				throw log.unableToInitializeMongoDB( e );
			}
			mongoDb = extractDatabase();
		}
	}

	@Override
	public void stop() {
		log.disconnectingFromMongo();
		this.mongo.close();
	}

	public DB getDatabase() {
		return mongoDb;
	}

	private DB extractDatabase() {
		try {
			if ( config.getUsername() != null ) {
				DB admin = this.mongo.getDB( "admin" );
				boolean auth = admin.authenticate( config.getUsername(), config.getPassword().toCharArray() );
				if ( !auth ) {
					throw log.authenticationFailed( config.getUsername() );
				}
			}
			if ( !this.mongo.getDatabaseNames().contains( config.getDatabaseName() ) ) {
				log.creatingDatabase( config.getDatabaseName() );
			}
			return this.mongo.getDB( config.getDatabaseName() );
		}
		catch ( HibernateException e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw log.unableToConnectToDatastore( this.config.getHost(), this.config.getPort(), e );
		}
	}

	@Override
	public Iterator<Tuple> executeBackendQuery(CustomQuery customQuery, EntityKeyMetadata[] metadatas) {
		BasicDBObject mongodbQuery = (BasicDBObject) com.mongodb.util.JSON.parse( customQuery.getSQL() );
		validate( metadatas );
		DBCollection collection = getDatabase().getCollection( metadatas[0].getTable() );
		DBCursor cursor = collection.find( mongodbQuery );
		return new MongoDBResultsCursor( cursor, metadatas[0] );
	}

	private void validate(EntityKeyMetadata[] metadatas) {
		if ( metadatas.length != 1 ) {
			throw log.requireMetadatas();
		}
	}

	private static class MongoDBResultsCursor implements Iterator<Tuple>, Closeable {

		private final DBCursor cursor;
		private final EntityKeyMetadata metadata;

		public MongoDBResultsCursor(DBCursor cursor, EntityKeyMetadata metadata) {
			this.cursor = cursor;
			this.metadata = metadata;
		}

		@Override
		public boolean hasNext() {
			return cursor.hasNext();
		}

		@Override
		public Tuple next() {
			DBObject dbObject = cursor.next();
			return new Tuple( new MassIndexingMongoDBTupleSnapshot( dbObject, metadata ) );
		}

		@Override
		public void remove() {
			cursor.remove();
		}

		@Override
		public void close() throws IOException {
			cursor.close();
		}

	}

}
