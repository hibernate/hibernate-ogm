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
public enum DatastoreProviderType {
	MAP( "org.hibernate.ogm.datastore.map.impl.MapDatastoreProvider" ),
	INFINISPAN( "org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider" ),
	INFINISPAN_REMOTE( "org.hibernate.ogm.datastore.infinispanremote.impl.InfinispanRemoteDatastoreProvider" ),
	EHCACHE( "org.hibernate.ogm.datastore.ehcache.impl.EhcacheDatastoreProvider" ),
	MONGODB( "org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider" ),
	FONGO( "org.hibernate.ogm.datastore.mongodb.impl.FongoDBDatastoreProvider" ),
	NEO4J_REMOTE( "org.hibernate.ogm.datastore.neo4j.remote.impl.RemoteNeo4jDatastoreProvider" ),
	NEO4J_EMBEDDED( "org.hibernate.ogm.datastore.neo4j.embedded.impl.EmbeddedNeo4jDatastoreProvider" ),
	COUCHDB_EXPERIMENTAL( "org.hibernate.ogm.datastore.couchdb.impl.CouchDBDatastoreProvider" ),
	CASSANDRA_EXPERIMENTAL( "org.hibernate.ogm.datastore.cassandra.impl.CassandraDatastoreProvider" ),
	REDIS_EXPERIMENTAL( "org.hibernate.ogm.datastore.redis.impl.RedisDatastoreProvider" );

	private String datastoreProviderClassName;

	private DatastoreProviderType(String datastoreProviderClassName) {
		this.datastoreProviderClassName = datastoreProviderClassName;
	}

	public String getDatastoreProviderClassName() {
		return this.datastoreProviderClassName;
	}

	public static boolean isShortName(String name) {
		for ( DatastoreProviderType provider : DatastoreProviderType.values() ) {
			if ( provider.name().equalsIgnoreCase( name ) ) {
				return true;
			}
		}
		return false;
	}

	public static DatastoreProviderType byShortName(String shortName) {
		return DatastoreProviderType.valueOf( shortName.toUpperCase() );
	}
}
