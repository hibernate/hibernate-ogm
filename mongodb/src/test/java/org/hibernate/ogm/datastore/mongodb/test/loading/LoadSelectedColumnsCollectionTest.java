/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.loading;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.util.impl.TransactionContextHelper.transactionContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.document.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBAssociationSnapshot;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBTupleSnapshot;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentStorageType;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.impl.AssociationContextImpl;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.model.impl.DefaultAssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultAssociationKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKind;
import org.hibernate.ogm.model.key.spi.AssociationType;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
import org.hibernate.ogm.options.navigation.impl.OptionsContextImpl;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSources;
import org.hibernate.ogm.options.spi.Option;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.options.spi.UniqueOption;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.ogm.utils.EmptyOptionsContext;
import org.hibernate.ogm.utils.GridDialectOperationContexts;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.Test;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 */
public class LoadSelectedColumnsCollectionTest extends OgmTestCase {

	@Test
	public void testLoadSelectedColumns() {
		final String collectionName = "Drink";

		MongoDBDatastoreProvider provider = (MongoDBDatastoreProvider) this.getService( DatastoreProvider.class );

		MongoDatabase database = provider.getDatabase();
		MongoCollection<Document> collection = database.getCollection( collectionName );
		Document water = new Document();
		water.put( "_id", "1234" );
		water.put( "name", "Water" );
		water.put( "volume", "1L" );
		collection.insertOne( water );

		List<String> selectedColumns = new ArrayList<>();
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

		collection.deleteMany( water );
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

		List<Module> modules = new ArrayList<>();
		modules.add( mongodb );
		modules.add( infinispan );

		Project hibernateOGM = new Project();
		hibernateOGM.setId( "projectID" );
		hibernateOGM.setName( "HibernateOGM" );
		hibernateOGM.setModules( modules );

		session.persist( hibernateOGM );
		transaction.commit();

		this.addExtraColumn();
		AssociationKeyMetadata metadata = new DefaultAssociationKeyMetadata.Builder()
				.table( "Project_Module" )
				.columnNames( new String[] { "Project_id" } )
				.rowKeyColumnNames( new String[] { "Project_id", "module_id" } )
				.associatedEntityKeyMetadata( new DefaultAssociatedEntityKeyMetadata( new String[] { "module_id" }, new DefaultEntityKeyMetadata( "Module", new String[] { "id" } ) ) )
				.inverse( false )
				.collectionRole( "modules" )
				.associationKind( AssociationKind.ASSOCIATION )
				.associationType( AssociationType.LIST )
				.build();

		AssociationKey associationKey = new AssociationKey(
				metadata,
				new Object[] { "projectID" },
				new EntityKey(
						new DefaultEntityKeyMetadata( "Project", new String[] { "id" } ),
						new String[] { "projectID" }
				)
		);

		AssociationTypeContext associationTypeContext = new AssociationTypeContext() {

			@Override
			public String getRoleOnMainSide() {
				return null;
			}

			@Override
			public TupleTypeContext getHostingEntityTupleTypeContext() {
				return GridDialectOperationContexts.emptyTupleTypeContext();
			}

			@Override
			public OptionsContext getHostingEntityOptionsContext() {
				return EmptyOptionsContext.INSTANCE;
			}

			@Override
			public OptionsContext getOptionsContext() {
				return OptionsContextImpl.forProperty(
								OptionValueSources.getDefaultSources(
										new ConfigurationPropertyReader( getSessionFactory().getProperties(), new ClassLoaderServiceImpl() ) ),
								Project.class,
								"modules" );
			}

			@Override
			public AssociatedEntityKeyMetadata getAssociatedEntityKeyMetadata() {
				return new DefaultAssociatedEntityKeyMetadata( null, null );
			}
		};

		AssociationContext associationContext = new AssociationContextImpl(
				associationTypeContext,
				new TuplePointer( new Tuple( new MongoDBTupleSnapshot( null, null ), SnapshotType.UPDATE ) ),
				transactionContext( session )
		);

		final Association association = getService( GridDialect.class ).getAssociation( associationKey, associationContext );
		final MongoDBAssociationSnapshot associationSnapshot = (MongoDBAssociationSnapshot) association.getSnapshot();
		final Document assocObject = associationSnapshot.getDocument();
		this.checkLoading( assocObject );

		session.delete( mongodb );
		session.delete( infinispan );
		session.delete( hibernateOGM );
		session.close();
	}

	private Tuple getTuple(String collectionName, String id, List<String> selectedColumns) {
		EntityKey key = new EntityKey(
				new DefaultEntityKeyMetadata( collectionName, new String[] { MongoDBDialect.ID_FIELDNAME } ),
				new Object[] { id }
		);
		TupleContext tupleContext = new GridDialectOperationContexts.TupleContextBuilder()
				.tupleTypeContext(
						new GridDialectOperationContexts.TupleTypeContextBuilder()
								.selectableColumns( selectedColumns )
								.optionContext( TestOptionContext.INSTANCE )
								.buildTupleTypeContext() )
				.buildTupleContext();

		return getService( GridDialect.class ).getTuple( key, tupleContext );
	}

	protected <S extends Service> S getService(Class<S> serviceRole) {
		SessionFactoryImplementor factory = super.getSessionFactory();
		ServiceRegistryImplementor serviceRegistry = factory.getServiceRegistry();
		return serviceRegistry.getService( serviceRole );
	}

	@Override
	protected void configure(Map<String, Object> settings) {
		settings.put(
				DocumentStoreProperties.ASSOCIATIONS_STORE,
				AssociationStorageType.ASSOCIATION_DOCUMENT
		);
		settings.put(
				MongoDBProperties.ASSOCIATION_DOCUMENT_STORAGE,
				AssociationDocumentStorageType.COLLECTION_PER_ASSOCIATION
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
		MongoDatabase database = provider.getDatabase();
		MongoCollection<Document> collection = database.getCollection( "associations_Project_Module" );
		Document query = new Document(  );
		query.put( "_id", new Document( "Project_id", "projectID" ) );

		Document updater = new Document(  );
		updater.put( "$push", new Document( "extraColumn", 1 ) );
		collection.updateMany( query, updater );
	}

	protected void checkLoading(Document associationObject) {
		/*
		 * The only column (except _id) that needs to be retrieved is "rows"
		 * So we should have 2 columns
		 */
		final Set<?> retrievedColumns = associationObject.keySet();
		assertThat( retrievedColumns ).hasSize( 2 ).containsOnly( MongoDBDialect.ID_FIELDNAME, MongoDBDialect.ROWS_FIELDNAME );
	}

	private static class TestOptionContext implements OptionsContext {

		public static OptionsContext INSTANCE = new TestOptionContext();

		@Override
		public <I, V, O extends Option<I, V>> V get(Class<O> optionType, I identifier) {
			try {
				Option<I,V> optionInstance = optionType.newInstance();
				return optionInstance.getDefaultValue( new ConfigurationPropertyReader( Collections.EMPTY_MAP ) );
			}
			catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException( e );
			}
		}

		@Override
		public <V, O extends UniqueOption<V>> V getUnique(Class<O> optionType) {
			try {
				UniqueOption<V> optionInstance = optionType.newInstance();
				return optionInstance.getDefaultValue( new ConfigurationPropertyReader( Collections.EMPTY_MAP ) );
			}
			catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException( e );
			}
		}

		@Override
		public <I, V, O extends Option<I, V>> Map<I, V> getAll(Class<O> optionType) {
			return null;
		}
	}
}
