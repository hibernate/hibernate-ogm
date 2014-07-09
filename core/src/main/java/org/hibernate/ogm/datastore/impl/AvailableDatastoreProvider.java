/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.impl;

/**
 * This enumeration describes all available datastore providers by providing some shortcuts.
 * It's used for the Datastore Provider initialization to find the provider to instantiate.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public enum AvailableDatastoreProvider {
	MAP( "org.hibernate.ogm.datastore.map.impl.MapDatastoreProvider" ),
	INFINISPAN( "org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider" ),
	EHCACHE( "org.hibernate.ogm.datastore.ehcache.impl.EhcacheDatastoreProvider" ),
	MONGODB( "org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider" ),
	NEO4J_EMBEDDED( "org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider" ),
	COUCHDB( "org.hibernate.ogm.datastore.couchdb.impl.CouchDBDatastoreProvider" );

	private String datastoreProviderClassName;

	private AvailableDatastoreProvider(String datastoreProviderClassName) {
		this.datastoreProviderClassName = datastoreProviderClassName;
	}

	public String getDatastoreProviderClassName() {
		return this.datastoreProviderClassName;
	}

	public static boolean isShortName(String name) {
		for ( AvailableDatastoreProvider provider : AvailableDatastoreProvider.values() ) {
			if ( provider.name().equalsIgnoreCase( name ) ) {
				return true;
			}
		}
		return false;
	}

	public static AvailableDatastoreProvider byShortName(String shortName) {
		return AvailableDatastoreProvider.valueOf( shortName.toUpperCase() );
	}
}
