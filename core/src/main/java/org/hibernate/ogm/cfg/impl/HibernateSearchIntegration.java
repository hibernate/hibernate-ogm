/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.cfg.impl;

import java.util.Map;
import java.util.Properties;

import org.hibernate.cfg.Configuration;

/**
 * Hibernate Search is an optional dependency, so while we need refer to its constants
 * to make sure its configured correctly for OGM by default, we don't want to create a
 * hard dependency. This class should be used as a helper to centralise that.
 *
 * @author Sanne Grinovero
 */
public final class HibernateSearchIntegration {

	private static final boolean searchIsAvailable = isSearchAvailable();
	private static String MASSINDEXER_PROPERTY_KEY = null;
	private static String MASSINDEXER_PROPERTY_VALUE = null;
	private static String RETRIEVALSTRATEGY_PROPERTY_KEY = null;
	private static String RETRIEVALSTRATEGY_PROPERTY_VALUE = null;

	static {
		if ( searchIsAvailable ) {
			RETRIEVALSTRATEGY_PROPERTY_KEY = org.hibernate.search.cfg.Environment.DATABASE_RETRIEVAL_METHOD;
			RETRIEVALSTRATEGY_PROPERTY_VALUE = org.hibernate.search.query.DatabaseRetrievalMethod.FIND_BY_ID.name();
			MASSINDEXER_PROPERTY_KEY = org.hibernate.search.batchindexing.spi.MassIndexerFactory.MASS_INDEXER_FACTORY_CLASSNAME;
			MASSINDEXER_PROPERTY_VALUE = org.hibernate.ogm.massindex.impl.OgmMassIndexerFactory.class.getName();
		}
	}

	private HibernateSearchIntegration() {
		// Not to be instantiated
	}

	private static boolean isSearchAvailable() {
		try {
			HibernateSearchIntegration.class.getClassLoader().loadClass( "org.hibernate.search.cfg.Environment" );
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	public static void resetProperties(Configuration ogmConfiguration) {
		if ( searchIsAvailable ) {
			// This property binds the OgmMassIndexer with Hibernate Search. An application could use OGM without Hibernate
			// Search therefore we set property value and key using a String in case the dependency is not on the classpath.
			ogmConfiguration.setProperty( MASSINDEXER_PROPERTY_KEY, MASSINDEXER_PROPERTY_VALUE );

			// Search needs to load matching entries by id, and not recursively with another query as it does by default.
			// Constants not used to avoid strictly depending on Search: org.hibernate.search.cfg.Environment.OBJECT_LOOKUP_METHOD
			ogmConfiguration.setProperty( RETRIEVALSTRATEGY_PROPERTY_KEY, RETRIEVALSTRATEGY_PROPERTY_VALUE );
		}
	}

	public static void setPropertiesIfUndefined(Properties properties, Configuration ogmConfiguration) {
		if ( searchIsAvailable ) {
			if ( ! properties.containsKey( MASSINDEXER_PROPERTY_KEY ) ) {
				ogmConfiguration.setProperty( MASSINDEXER_PROPERTY_KEY, MASSINDEXER_PROPERTY_VALUE );
			}
			if ( ! properties.containsKey( RETRIEVALSTRATEGY_PROPERTY_KEY ) ) {
				ogmConfiguration.setProperty( RETRIEVALSTRATEGY_PROPERTY_KEY, RETRIEVALSTRATEGY_PROPERTY_VALUE );
			}
		}
	}

	public static void resetProperties(Map<Object, Object> map) {
		if ( searchIsAvailable ) {
			// set the OGM specific mass indexer in case we use Hibernate Search
			map.put( MASSINDEXER_PROPERTY_KEY, MASSINDEXER_PROPERTY_VALUE );
			// set the Hibernate Search strategy to load query matches by id rather then recurse into another query
			map.put( RETRIEVALSTRATEGY_PROPERTY_KEY, RETRIEVALSTRATEGY_PROPERTY_VALUE );
		}
	}

}
