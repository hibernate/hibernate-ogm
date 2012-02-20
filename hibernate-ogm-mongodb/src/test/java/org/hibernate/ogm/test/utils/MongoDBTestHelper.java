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

package org.hibernate.ogm.test.utils;

import java.util.Iterator;
import java.util.Map;

import org.bson.types.ObjectId;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.mongodb.MongoDBDialect;
import org.hibernate.ogm.grid.EntityKey;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * 
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class MongoDBTestHelper implements TestableGridDialect {

	@Override
	public int entityCacheSize(SessionFactory sessionFactory) {
		MongoDBDatastoreProvider provider = MongoDBTestHelper.getProvider( sessionFactory );
		DB db = provider.getDatabase();
		int count = 0;
		for ( String collectionName : db.getCollectionNames() ) {
			count += db.getCollection( collectionName ).count();
		}
		return count;
	}

	private int countAssociationOnCollection(DBCollection collection) {
		DBCursor cursor = collection.find( new BasicDBObject(), new BasicDBObject( MongoDBDialect.ID_FIELDNAME, 0 ) );
		Iterator<DBObject> it = cursor.iterator();
		int count = 0;
		while ( it.hasNext() ) {
			DBObject current = it.next();
			Map<?, ?> map = current.toMap();
			count += this.countAssociationOnDocument( map );
		}
		return count;
	}

	private int countAssociationOnDocument(Map<?, ?> map) {
		int count = 0;
		for ( Object key : map.keySet() ) {
			Object value = map.get( key );
			if ( value instanceof Map ) {
				count += this.countAssociationOnDocument( (Map<?, ?>) value );
			}
			else {
				count += map.get( key ).getClass().equals( ObjectId.class ) ? 1 : 0;
			}
		}
		return count;
	}

	@Override
	public int associationCacheSize(SessionFactory sessionFactory) {
		MongoDBDatastoreProvider provider = MongoDBTestHelper.getProvider( sessionFactory );
		DB db = provider.getDatabase();
		int generalCount = 0;
		for ( String collectionName : db.getCollectionNames() ) {
			generalCount += this.countAssociationOnCollection( db.getCollection( collectionName ) );
		}
		return generalCount;
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		MongoDBDatastoreProvider provider = MongoDBTestHelper.getProvider( sessionFactory );
		DBObject finder = new BasicDBObject( MongoDBDialect.ID_FIELDNAME, key.getColumnValues()[0] );
		DBObject result = provider.getDatabase().getCollection( key.getTable() ).findOne( finder );
		return result.toMap();
	}

	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}

	private static MongoDBDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService(
				DatastoreProvider.class );
		if ( !( MongoDBDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with MongoDB, cannot extract underlying cache" );
		}
		return MongoDBDatastoreProvider.class.cast( provider );
	}

}
