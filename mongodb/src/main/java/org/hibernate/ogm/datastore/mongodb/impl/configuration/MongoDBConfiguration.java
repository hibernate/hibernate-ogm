/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012-2014 Red Hat Inc. and/or its affiliates and other contributors
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
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.ogm.cfg.impl.DocumentStoreConfiguration;
import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentType;
import org.hibernate.ogm.datastore.mongodb.options.ReadPreferenceType;
import org.hibernate.ogm.datastore.mongodb.options.WriteConcernType;
import org.hibernate.ogm.datastore.mongodb.options.impl.ReadPreferenceOption;
import org.hibernate.ogm.datastore.mongodb.options.impl.WriteConcernOption;
import org.hibernate.ogm.options.spi.OptionsContainer;
import org.hibernate.ogm.util.configurationreader.impl.ConfigurationPropertyReader;
import org.hibernate.ogm.util.configurationreader.impl.PropertyValidator;

import com.mongodb.MongoClientOptions;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;

/**
 * Configuration for {@link MongoDBDatastoreProvider}.
 *
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 * @author Gunnar Morling
 */
public class MongoDBConfiguration extends DocumentStoreConfiguration {

	public static final String DEFAULT_ASSOCIATION_STORE = "Associations";

	/**
	 * The default write concern.
	 *
	 * @see MongoDBProperties#WRITE_CONCERN
	 */
	private static final WriteConcernType DEFAULT_WRITE_CONCERN = WriteConcernType.ACKNOWLEDGED;

	/**
	 * The default read preference.
	 *
	 * @see MongoDBProperties#READ_PREFERENCE
	 */
	private static final ReadPreferenceType DEFAULT_READ_PREFERENCE = ReadPreferenceType.PRIMARY;

	/**
	 * The default value used to set the timeout during the connection to the MongoDB instance This value is set in
	 * milliseconds.
	 *
	 * @see MongoDBProperties#TIMEOUT
	 */
	private static final int DEFAULT_TIMEOUT = 5000;

	private static final int DEFAULT_PORT = 27017;

	private static final Log log = LoggerFactory.getLogger();

	private static final TimeoutValidator TIMEOUT_VALIDATOR = new TimeoutValidator();

	private final AssociationDocumentType associationDocumentStorage;
	private final int timeout;
	private final WriteConcern writeConcern;
	private final ReadPreference readPreference;

	/**
	 * Creates a new {@link MongoDBConfiguration}.
	 *
	 * @param configurationValues configuration values given via {@code persistence.xml} etc.
	 * @param globalOptions global settings given via an option configurator
	 * @param classLoaderService class loader service for loading classes by name
	 */
	public MongoDBConfiguration(Map<?, ?> configurationValues, OptionsContainer globalOptions, ClassLoaderService classLoaderService) {
		super( configurationValues, DEFAULT_PORT );

		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configurationValues );

		this.timeout = propertyReader.property( MongoDBProperties.TIMEOUT, int.class )
				.withDefault( DEFAULT_TIMEOUT )
				.withValidator( TIMEOUT_VALIDATOR )
				.getValue();

		this.associationDocumentStorage = propertyReader.property( MongoDBProperties.ASSOCIATION_DOCUMENT_STORAGE, AssociationDocumentType.class )
				.withDefault( AssociationDocumentType.GLOBAL_COLLECTION )
				.getValue();

		this.writeConcern = this.buildWriteConcern( propertyReader, globalOptions.getUnique( WriteConcernOption.class ), classLoaderService );

		ReadPreference apiConfiguredReadPreference = globalOptions.getUnique( ReadPreferenceOption.class );
		if ( apiConfiguredReadPreference != null ) {
			readPreference = apiConfiguredReadPreference;
		}
		else {
			readPreference = propertyReader
				.property( MongoDBProperties.READ_PREFERENCE, ReadPreferenceType.class )
				.withDefault( DEFAULT_READ_PREFERENCE )
				.getValue()
				.getReadPreference();
		}
	}

	/**
	 * @see MongoDBProperties#ASSOCIATION_DOCUMENT_STORAGE
	 * @return how to store association documents
	 */
	public AssociationDocumentType getAssociationDocumentStorage() {
		return associationDocumentStorage;
	}

	public WriteConcern getWriteConcern() {
		return writeConcern;
	}

	public ReadPreference getReadPreference() {
		return readPreference;
	}

	private WriteConcern buildWriteConcern(ConfigurationPropertyReader propertyReader, WriteConcern apiConfiguredWriteConcern, ClassLoaderService classLoaderService) {
		WriteConcern writeConcern;

		// API-configured value takes precedence
		if ( apiConfiguredWriteConcern != null ) {
			writeConcern = apiConfiguredWriteConcern;
		}
		else {
			WriteConcernType writeConcernType = propertyReader.property( MongoDBProperties.WRITE_CONCERN, WriteConcernType.class )
				.withDefault( DEFAULT_WRITE_CONCERN )
				.getValue();

			// load/instantiate custom type
			if ( writeConcernType == WriteConcernType.CUSTOM ) {
				writeConcern = propertyReader.property( MongoDBProperties.WRITE_CONCERN_TYPE, WriteConcern.class )
					.instantiate()
					.withClassLoaderService( classLoaderService )
					.required()
					.getValue();
			}
			// take pre-defined value
			else {
				writeConcern = writeConcernType.getWriteConcern();
			}
		}

		log.usingWriteConcern(
				writeConcern.getWString(),
				writeConcern.getWtimeout(),
				writeConcern.getFsync(),
				writeConcern.getJ(),
				writeConcern.getContinueOnErrorForInsert()
		);

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
		optionsBuilder.readPreference( readPreference );

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
