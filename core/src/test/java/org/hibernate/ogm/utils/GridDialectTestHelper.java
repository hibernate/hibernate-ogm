/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.EntityKey;

/**
 * For testing purposes we need to be able to extract more information than what is mandated from the GridDialect,
 * so for each GridDialect implementor we need to implement a GridDialectTestHelper, and list it in
 * {@code org.hibernate.ogm.utils.GridModule }.
 *
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 */
public interface GridDialectTestHelper {

	/**
	 * Returns the number of entities in the datastore
	 *
	 * @param session
	 */
	long getNumberOfEntities(Session session);

	/**
	 * Returns the number of entities in the datastore
	 *
	 * @param sessionFactory
	 */
	long getNumberOfEntities(SessionFactory sessionFactory);

	/**
	 * Returns the number of associations in the datastore
	 *
	 * @param session
	 */
	long getNumberOfAssociations(Session session);

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
	 * @param session
	 * @param key
	 * @return the loaded tuple, or null of nothing was found
	 */
	Map<String, Object> extractEntityTuple(Session session, EntityKey key);

	/**
	 * Returning false will disable all tests which verify transaction isolation or rollback capabilities.
	 * No "production" datastore should return false unless its limitation is properly documented.
	 *
	 * @return true if the datastore is expected to commit/rollback properly
	 */
	boolean backendSupportsTransactions();

	/**
	 * Initialize the database on start-up.
	 *
	 * @param sessionFactory
	 */
	void prepareDatabase(SessionFactory sessionFactory);

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
	 * Properties that needs to be added to the configuration for tests to run,
	 * for example Neo4J will generate the database store path programmatically
	 * so expecting it in a configuration file is not practical.
	 */
	Map<String, String> getAdditionalConfigurationProperties();

	/**
	 * Returns the store-specific {@link DatastoreConfiguration} type for applying configuration options.
	 *
	 * @return the store-specific {@link DatastoreConfiguration} type
	 */
	Class<? extends DatastoreConfiguration<?>> getDatastoreConfigurationType();

	GridDialect getGridDialect(DatastoreProvider datastoreProvider);
}
