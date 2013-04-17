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

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.impl.DatastoreServices;
import org.hibernate.ogm.datastore.mongodb.AssociationStorage;
import org.hibernate.ogm.datastore.mongodb.Environment;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.mongodb.MongoDBAssociationSnapshot;
import org.hibernate.ogm.dialect.mongodb.MongoDBDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.AssociationKeyMetadata;
import org.hibernate.ogm.grid.AssociationKind;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.test.simpleentity.OgmTestCase;
import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


/**
 * @author Guillaume Scheibel<guillaume.scheibel@gmail.com>
 */
public class LoadSelectedColumnsCollectionTest extends OgmTestCase {

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

		List<String> selectedColumns = new ArrayList<String>();
		selectedColumns.add( "name" );
		Tuple tuple = this.getTuple( collectionName, "1234", selectedColumns );

		assertNotNull( tuple );
		Set<String> retrievedColumn = tuple.getColumnNames();

		/*
		 * The dialect will return all columns (which include _id field) so we have to substract 1 to check if
		 * the right number of columns has been loaded.
		 */
		assertEquals( selectedColumns.size(), retrievedColumn.size() - 1 );
		assertTrue( retrievedColumn.containsAll( selectedColumns ) );

		collection.remove( water );
	}

	@Test
	public void testLoadSelectedAssociationColumns() {
		Session session = openSession();
		final Transaction transaction = session.getTransaction();
		transaction.begin();

		Module mongodb = new Module();
		mongodb.setName( "MongoDB" );
		session.persist( mongodb );

		Module infinispan = new Module();
		infinispan.setName( "Infinispan" );
		session.persist( infinispan );

		List<Module> modules = new ArrayList<Module>();
		modules.add( mongodb );
		modules.add( infinispan );

		Project hibernateOGM = new Project();
		hibernateOGM.setId( "projectID" );
		hibernateOGM.setName( "HibernateOGM" );
		hibernateOGM.setModules( modules );

		session.persist( hibernateOGM );
		transaction.commit();

		this.addExtraColumn();
		GridDialect gridDialect = this.getGridDialect();
		AssociationKeyMetadata metadata = new AssociationKeyMetadata( "Project_Module", new String[] { "Project_id" } );
		metadata.setRowKeyColumnNames( new String[] { "Project_id", "module_id" } );
		AssociationKey associationKey = new AssociationKey( metadata, new Object[] { "projectID" } );
		associationKey.setAssociationKind( AssociationKind.ASSOCIATION );
		associationKey.setCollectionRole( "modules" );
		associationKey.setOwnerEntityKey( new EntityKey( new EntityKeyMetadata( "Project", new String[] { "id" } ), new String[] { "projectID" } ) );
		AssociationContext associationContext = new AssociationContext( Arrays.asList( associationKey.getRowKeyColumnNames() ) );
		final Association association = gridDialect.getAssociation( associationKey, associationContext );
		final MongoDBAssociationSnapshot associationSnapshot = (MongoDBAssociationSnapshot) association.getSnapshot();
		final DBObject assocObject = associationSnapshot.getDBObject();
		this.checkLoading( assocObject );

		session.delete( mongodb );
		session.delete( infinispan );
		session.delete( hibernateOGM );
		session.close();
	}

	public Tuple getTuple(String collectionName, String id, List<String> selectedColumns) {
		EntityKey key = new EntityKey( new EntityKeyMetadata( collectionName,
				new String[] { MongoDBDialect.ID_FIELDNAME } ), new Object[] { id } );
		TupleContext tupleContext = new TupleContext( selectedColumns );
		return this.getGridDialect().getTuple( key, tupleContext );
	}

	protected Service getService(Class<? extends Service> serviceImpl) {
		SessionFactoryImplementor factory = super.sfi();
		ServiceRegistryImplementor serviceRegistry = factory.getServiceRegistry();
		return serviceRegistry.getService( serviceImpl );
	}

	protected GridDialect getGridDialect() {
		return ( (DatastoreServices) this.getService( DatastoreServices.class ) ).getGridDialect();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.MONGODB_ASSOCIATIONS_STORE, AssociationStorage.COLLECTION.name() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Project.class, Module.class };
	}

	/**
	 * To be sure the datastoreProvider retrieves only the columns we want,
	 * an extra column is manually added to the association document
	 */
	protected void addExtraColumn() {
		MongoDBDatastoreProvider provider = (MongoDBDatastoreProvider) this.getService( DatastoreProvider.class );
		DB database = provider.getDatabase();
		DBCollection collection = database.getCollection( "associations_Project_Module" );
		BasicDBObject query = new BasicDBObject( 1 );
		query.put( "_id", new BasicDBObject( "Project_id", "projectID" ) );

		BasicDBObject updater = new BasicDBObject( 1 );
		updater.put( "$push", new BasicDBObject( "extraColumn", 1 ) );
		collection.update( query, updater );
	}

	protected void checkLoading(DBObject associationObject) {
		/*
		 * The only column (except _id) that needs to be retrieved is "rows"
		 * So we should have 2 columns
		 */
		final Set<?> retrievedColumns = associationObject.keySet();
		assertThat( retrievedColumns ).hasSize( 2 ).containsOnly( MongoDBDialect.ID_FIELDNAME, MongoDBDialect.ROWS_FIELDNAME );
	}
}
