/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.mongodb.impl.configuration;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.ogm.cfg.impl.DocumentStoreConfiguration;
import org.hibernate.ogm.datastore.mongodb.AssociationDocumentType;
import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.logging.mongodb.impl.Log;
import org.hibernate.ogm.logging.mongodb.impl.LoggerFactory;
import org.hibernate.ogm.util.impl.configurationreader.ConfigurationPropertyReader;
import org.hibernate.ogm.util.impl.configurationreader.PropertyValidator;

import com.mongodb.MongoClientOptions;
import com.mongodb.WriteConcern;

/**
 * Configuration for {@link org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider}.
 *
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class MongoDBConfiguration extends DocumentStoreConfiguration {

	/**
	 * The default value used to set up the acknowledgement of write operations.
	 *
	 * @see MongoDBProperties#WRITE_CONCERN
	 */
	public static final WriteConcern DEFAULT_WRITE_CONCERN = WriteConcern.ACKNOWLEDGED;

	public static final String DEFAULT_ASSOCIATION_STORE = "Associations";

	/**
	 * The default value used to set the timeout during the connection to the MongoDB instance This value is set in
	 * milliseconds.
	 *
	 * @see MongoDBProperties#TIMEOUT
	 */
	public static final int DEFAULT_TIMEOUT = 5000;

	private static final int DEFAULT_PORT = 27017;

	private static final Log log = LoggerFactory.getLogger();

	private static final TimeoutValidator TIMEOUT_VALIDATOR = new TimeoutValidator();

	private final AssociationDocumentType associationDocumentStorage;
	private final int timeout;
	private final WriteConcern writeConcern;

	public MongoDBConfiguration(Map<?, ?> configurationValues) {
		super( configurationValues, DEFAULT_PORT );

		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configurationValues );

		this.timeout = propertyReader.property( MongoDBProperties.TIMEOUT, int.class )
				.withDefault( DEFAULT_TIMEOUT )
				.withValidator( TIMEOUT_VALIDATOR )
				.getValue();

		this.associationDocumentStorage = propertyReader.property( MongoDBProperties.ASSOCIATION_DOCUMENT_STORAGE, AssociationDocumentType.class )
				.withDefault( AssociationDocumentType.GLOBAL_COLLECTION )
				.getValue();

		this.writeConcern = this.buildWriteConcern( configurationValues );
	}

	/**
	 * @see MongoDBProperties#ASSOCIATION_DOCUMENT_STORAGE
	 * @return how to store association documents
	 */
	public AssociationDocumentType getAssociationDocumentStorage() {
		return associationDocumentStorage;
	}

	private WriteConcern buildWriteConcern(Map<?, ?> cfg) {
		Object cfgWriteConcern = cfg.get( MongoDBProperties.WRITE_CONCERN );
		WriteConcern writeConcern = DEFAULT_WRITE_CONCERN;
		String wcLogMessage = "ACKNOWLEDGED";
		if ( cfgWriteConcern != null ) {
			final String confWC = cfgWriteConcern.toString();
			writeConcern = WriteConcern.valueOf( confWC );

			if ( writeConcern == null ) {
				writeConcern = DEFAULT_WRITE_CONCERN;
				wcLogMessage = "ACKNOWLEDGED";
			}
			else {
				wcLogMessage = confWC;
			}
		}
		// using a custom string representation because neither toString() nor getWString() return a user-friendly message
		log.useWriteConcern( wcLogMessage );
		return writeConcern;
	}

	/**
	 * Create a {@link MongoClientOptions} using the {@link MongoDBConfiguration}.
	 *
	 * @return the {@link MongoClientOptions} corresponding to the {@link MongoDBConfiguration}
	 */
	public MongoClientOptions buildOptions() {
		MongoClientOptions.Builder optionsBuilder = new MongoClientOptions.Builder();
		optionsBuilder.connectTimeout( timeout );
		optionsBuilder.writeConcern( writeConcern );

		return optionsBuilder.build();
	}

	private static class TimeoutValidator implements PropertyValidator<Integer> {

		@Override
		public void validate(Integer value) throws HibernateException {
			if ( value < 0 ) {
				throw log.mongoDBTimeOutIllegalValue( value );
			}
		}
	}
}
