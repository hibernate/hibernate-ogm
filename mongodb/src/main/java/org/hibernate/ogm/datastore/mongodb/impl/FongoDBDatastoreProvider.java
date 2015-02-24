/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.impl;

import org.hibernate.ogm.datastore.mongodb.configuration.impl.MongoDBConfiguration;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;

/**
 * Provides access to a FongoDB instance
 *
 * @author Alex Soto
 */
public class FongoDBDatastoreProvider extends MongoDBDatastoreProvider {

	private Fongo fongo;

	@Override
	protected MongoClient createMongoClient(MongoDBConfiguration config) {
		fongo = new Fongo( "Fongo Hibernate OGM" );
		return fongo.getMongo();
	}
}
