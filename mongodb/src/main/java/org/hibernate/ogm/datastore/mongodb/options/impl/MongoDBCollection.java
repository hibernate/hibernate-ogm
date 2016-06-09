/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.util.impl.StringHelper;

/**
 * MongoDB specific definitions for a collection.
 *
 * @author Guillaume Smet
 */
public class MongoDBCollection {

	private Map<String, MongoDBIndexOptions> indexOptions = new HashMap<>();

	public MongoDBCollection() {
	}

	MongoDBCollection(org.hibernate.ogm.datastore.mongodb.options.MongoDBCollection annotation) {
		for ( org.hibernate.ogm.datastore.mongodb.options.MongoDBIndexOptions optionsAnnotation : annotation.indexOptions() ) {
			indexOptions.put( optionsAnnotation.forIndex(), new MongoDBIndexOptions( optionsAnnotation ) );
		}
	}

	public MongoDBIndexOptions getOptionsForIndex(String indexName) {
		if ( StringHelper.isNullOrEmptyString( indexName ) ) {
			return new MongoDBIndexOptions();
		}
		if ( !indexOptions.containsKey( indexName ) ) {
			return new MongoDBIndexOptions( indexName );
		}

		return indexOptions.get( indexName );
	}

	public Set<String> getReferencedIndexes() {
		return Collections.unmodifiableSet( indexOptions.keySet() );
	}

}
