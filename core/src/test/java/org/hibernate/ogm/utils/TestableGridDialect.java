/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.options.navigation.GlobalContext;

/**
 * For testing purposes we need to be able to extract more information than what is mandated from the GridDialect,
 * so each GridDialect implementor should also implement a TestGridDialect, and list it by classname into
 * {@code org.hibernate.ogm.test.utils.TestHelper#knownTestDialects }.
 *
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 */
public interface TestableGridDialect {

	/**
	 * Returns the number of entities in the datastore
	 *
	 * @param sessionFactory
	 */
	long getNumberOfEntities(SessionFactory sessionFactory);

	/**
	 * Returns the number of associations in the datastore
	 *
	 * @param sessionFactory
	 */
	long getNumberOfAssociations(SessionFactory sessionFactory);

	/**
	 * Returns the number of associations of the given type in the datastore
	 *
	 * @param sessionFactory factory used to connect to the store
	 * @param type the association type of interest
	 * @return the number of associations of the given type
	 */
	long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type);

	/**
	 * Loads a specific entity tuple directly from the data store by entity key
	 *
	 * @param sessionFactory
	 * @param key
	 * @return the loaded tuple, or null of nothing was found
	 */
	Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key);

	/**
	 * Returning false will disable all tests which verify transaction isolation or rollback capabilities.
	 * No "production" datastore should return false unless its limitation is properly documented.
	 *
	 * @return true if the datastore is expected to commit/rollback properly
	 */
	boolean backendSupportsTransactions();

	/**
	 * Used to clean up all the stored data. The cleaning can be done by dropping
	 * the database and/or the schema.
	 * Each implementor can so define its own way to delete all data inserted by
	 * the test and remove the schema if that applies.
	 *
	 * @param sessionFactory
	 */
	void dropSchemaAndDatabase(SessionFactory sessionFactory);

	/**
	 * Properties that needs to be overridden in configuration for tests to run
	 * This is typical of the host and port defined using an environment variable.
	 */
	Map<String, String> getEnvironmentProperties();

	/**
	 * Returns the store-specific {@link GlobalContext} for applying configuration options.
	 *
	 * @param configuration the {@link OgmConfiguration} to which the options should be applied to
	 * @return the store-specific {@link GlobalContext}
	 */
	GlobalContext<?, ?> configureDatastore(OgmConfiguration configuration);

	GridDialect getGridDialect(DatastoreProvider datastoreProvider);
}
