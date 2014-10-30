/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.loading;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBAssociationSnapshot;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBTupleSnapshot;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentType;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.impl.AssociationContextImpl;
import org.hibernate.ogm.dialect.impl.AssociationTypeContextImpl;
import org.hibernate.ogm.dialect.impl.TupleContextImpl;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.AssociationKind;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.options.navigation.impl.OptionsContextImpl;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSources;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.ogm.utils.EmptyOptionsContext;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
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
		AssociationKeyMetadata metadata = new AssociationKeyMetadata.Builder()
				.table( "Project_Module" )
				.columnNames( new String[] { "Project_id" } )
				.rowKeyColumnNames( new String[] { "Project_id", "module_id" } )
				.associatedEntityKeyMetadata( new AssociatedEntityKeyMetadata( new String[] { "module_id" }, new EntityKeyMetadata( "Module", new String[] { "id" } ) ) )
				.inverse( false )
				.collectionRole( "modules" )
				.associationKind( AssociationKind.ASSOCIATION )
				.oneToOne( false )
				.build();

		AssociationKey associationKey = new AssociationKey(
				metadata,
				new Object[] { "projectID" },
				new EntityKey(
						new EntityKeyMetadata( "Project", new String[] { "id" } ),
						new String[] { "projectID" }
				)
		);

		AssociationContext associationContext = new AssociationContextImpl(
				new AssociationTypeContextImpl(
						OptionsContextImpl.forProperty(
								OptionValueSources.getDefaultSources( new ConfigurationPropertyReader( sessions.getProperties(), new ClassLoaderServiceImpl() ) ),
								Project.class,
								"modules"
						),
						new AssociatedEntityKeyMetadata( null, null ),
						null
				),
				new Tuple( new MongoDBTupleSnapshot( null, null, null ) )
		);

		final Association association = getService( GridDialect.class ).getAssociation( associationKey, associationContext );
		final MongoDBAssociationSnapshot associationSnapshot = (MongoDBAssociationSnapshot) association.getSnapshot();
		final DBObject assocObject = associationSnapshot.getDBObject();
		this.checkLoading( assocObject );

		session.delete( mongodb );
		session.delete( infinispan );
		session.delete( hibernateOGM );
		session.close();
	}

	private Tuple getTuple(String collectionName, String id, List<String> selectedColumns) {
		EntityKey key = new EntityKey(
				new EntityKeyMetadata( collectionName, new String[] { MongoDBDialect.ID_FIELDNAME } ),
				new Object[] { id }
		);
		TupleContext tupleContext = new TupleContextImpl(
				selectedColumns,
				Collections.<String, AssociatedEntityKeyMetadata>emptyMap(),
				Collections.<String, String>emptyMap(),
				EmptyOptionsContext.INSTANCE
		);

		return getService( GridDialect.class ).getTuple( key, tupleContext );
	}

	protected <S extends Service> S getService(Class<S> serviceRole) {
		SessionFactoryImplementor factory = super.sfi();
		ServiceRegistryImplementor serviceRegistry = factory.getServiceRegistry();
		return serviceRegistry.getService( serviceRole );
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.getProperties().put(
				DocumentStoreProperties.ASSOCIATIONS_STORE,
				AssociationStorageType.ASSOCIATION_DOCUMENT
		);
		cfg.getProperties().put(
				MongoDBProperties.ASSOCIATION_DOCUMENT_STORAGE,
				AssociationDocumentType.COLLECTION_PER_ASSOCIATION
		);
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
