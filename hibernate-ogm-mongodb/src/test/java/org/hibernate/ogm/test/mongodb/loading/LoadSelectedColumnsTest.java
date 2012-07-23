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
package org.hibernate.ogm.test.mongodb.loading;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.impl.DatastoreServices;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.test.simpleentity.OgmTestCase;
import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Guillaume Scheibel<guillaume.scheibel@gmail.com>
 */
public class LoadSelectedColumnsTest extends OgmTestCase {

	@Test
	public void testLoadSelectedColumns() {
		final String collectionName = "Drink";

		MongoDBDatastoreProvider provider = (MongoDBDatastoreProvider) this.getService( DatastoreProvider.class );

		DB database = provider.getDatabase();
		DBCollection collection = database.getCollection( collectionName );
		BasicDBObject water = new BasicDBObject();
		water.put( "_id", "1234" );
		water.put( "name", "Water" );
		water.put( "volume", "1L" );
		collection.insert( water );

		EntityKey key = new EntityKey( collectionName, new String[] { "_id" }, new Object[] { "1234" } );

		List<String> selectedColumns = new ArrayList<String>();
		selectedColumns.add( "name" );

		TupleContext tupleContext = new TupleContext( selectedColumns );

		Tuple tuple = this.getGridDialect().getTuple( key, tupleContext );
		assertNotNull( tuple );
		Set<String> retrievedColumn = tuple.getColumnNames();

		/*
		  *The dialect will return all columns (which include _id field) so we have to substract 1 to check if
		  *the right number of columns has been loaded.
		 */
		assertEquals( selectedColumns.size(), retrievedColumn.size() - 1 );
		assertTrue( retrievedColumn.containsAll( selectedColumns ) );

		collection.remove( water );
	}

	private Service getService(Class<? extends Service> serviceImpl){
		SessionFactoryImplementor factory = super.sfi();
		ServiceRegistryImplementor serviceRegistry = factory.getServiceRegistry();
		return serviceRegistry.getService( serviceImpl );
	}
	private GridDialect getGridDialect(){
		return (GridDialect) this.getService( DatastoreServices.class );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[0];
	}
}
