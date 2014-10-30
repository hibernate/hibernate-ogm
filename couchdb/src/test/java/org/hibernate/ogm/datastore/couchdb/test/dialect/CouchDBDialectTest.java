/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.test.dialect;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.couchdb.utils.CouchDBTestHelper.environmentProperties;
import static org.hibernate.ogm.datastore.couchdb.utils.CouchDBTestHelper.initEnvironmentProperties;
import static org.hibernate.ogm.util.impl.ArrayHelper.EMPTY_STRING_ARRAY;
import static org.hibernate.ogm.utils.GridDialectOperationContexts.emptyAssociationContext;
import static org.hibernate.ogm.utils.GridDialectOperationContexts.emptyTupleContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.ogm.datastore.couchdb.CouchDBDialect;
import org.hibernate.ogm.datastore.couchdb.dialect.model.impl.CouchDBTupleSnapshot;
import org.hibernate.ogm.datastore.couchdb.impl.CouchDBDatastoreProvider;
import org.hibernate.ogm.datastore.couchdb.utils.CouchDBTestHelper;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.AssociationKind;
import org.hibernate.ogm.model.spi.Tuple;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 */
public class CouchDBDialectTest {

	private final CouchDBDatastoreProvider datastoreProvider = new CouchDBDatastoreProvider();
	private CouchDBDialect dialect;

	static {
		initEnvironmentProperties();
	}

	@Before
	public void setUp() throws Exception {
		createDataStoreProvider();
		dialect = new CouchDBDialect( datastoreProvider );
	}

	@After
	public void tearDown() throws Exception {
		datastoreProvider.getDataStore().dropDatabase();
		datastoreProvider.stop();
	}

	@Test
	public void createTupleShouldReturnANewTuple() {

		EntityKey key = createEntityKey( "user", new String[] { "id", "age" }, new Object[] { "17", 36 } );
		Tuple createdTuple = dialect.createTuple( key, emptyTupleContext() );

		int actualIdValue = (Integer) createdTuple.get( "age" );
		assertThat( actualIdValue ).isEqualTo( 36 );
	}

	@Test
	public void getTupleShouldReturnTheSearchedOne() {
		EntityKey key = createEntityKey( "user", new String[] { "id", "age" }, new Object[] { "17", 36 } );
		Tuple createdTuple = dialect.createTuple( key, emptyTupleContext() );

		dialect.insertOrUpdateTuple( key, createdTuple, emptyTupleContext() );

		Tuple actualTuple = dialect.getTuple( key, emptyTupleContext() );

		assertThat( actualTuple.get( "id" ) ).isEqualTo( createdTuple.get( "id" ) );
	}

	@Test
	public void removeTupleShouldDeleteTheCreatedTuple() {
		EntityKey key = createEntityKey( "user", new String[] { "id", "age" }, new Object[] { "17", 36 } );
		dialect.createTuple( key, emptyTupleContext() );

		dialect.removeTuple( key, emptyTupleContext() );

		assertThat( new CouchDBTestHelper().getNumberOfEntities( datastoreProvider.getDataStore() ) ).isEqualTo( 0 );
	}

	@Test
	public void updateTupleShouldAddTheNewColumnValue() {

		EntityKey key = createEntityKey( "user", new String[] { "id", "age" }, new Object[] { "17", 36 } );
		Tuple createdTuple = dialect.createTuple( key, emptyTupleContext() );
		createdTuple.put( "name", "and" );

		dialect.insertOrUpdateTuple( key, createdTuple, emptyTupleContext() );

		Tuple tuple = dialect.getTuple( key, emptyTupleContext() );
		assertThat( (String) tuple.get( "name" ) ).isEqualTo( "and" );
	}

	@Test
	public void createAssociationShouldCreateAnEmptyAssociation() {
		Object[] columnValues = { "17" };
		String tableName = "user_address";
		String[] columnNames = { "id" };
		String[] rowKeyColumnNames = new String[] { "id" };
		EntityKey entityKey = createEntityKey( "user", new String[] { "id", "age" }, new Object[] { "17", 36 } );
		String collectionRole = "addresses";

		AssociationKey key = createAssociationKey( entityKey, collectionRole, tableName, columnNames, columnValues, rowKeyColumnNames );

		Association createAssociation = dialect.createAssociation( key, emptyAssociationContext() );

		assertThat( createAssociation.getSnapshot() ).isNotNull();
		assertThat( createAssociation.getSnapshot().getRowKeys() ).isEmpty();
	}

	@Test
	public void updateAnAssociationShouldAddATuple() {
		String tableName = "user_address";
		String[] rowKeyColumnNames = new String[] { "user_id", "addresses_id" };
		Object[] rowKeyColumnValues = new Object[] { "Emmanuel", 1 };
		EntityKey entityKey = createEntityKey( "user", new String[] { "id", "age" }, new Object[] { "17", 36 } );
		Tuple tuple = dialect.createTuple( entityKey, emptyTupleContext() );
		dialect.insertOrUpdateTuple( entityKey, tuple, emptyTupleContext() );

		AssociationKey key = createAssociationKey(
				entityKey, "addresses", "user_address", new String[] { "user_id" }, new Object[] { "Emmanuel" }, rowKeyColumnNames
		);
		Association createAssociation = dialect.createAssociation( key, emptyAssociationContext() );

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "user_id", "Emmanuel" );
		properties.put( "addresses_id", 1 );
		Tuple associationTuple = new Tuple( new CouchDBTupleSnapshot( properties ) );

		RowKey rowKey = new RowKey( rowKeyColumnNames, rowKeyColumnValues );
		createAssociation.put( rowKey, associationTuple );
		dialect.insertOrUpdateAssociation( key, createAssociation, emptyAssociationContext() );

		Association actualAssociation = dialect.getAssociation( key, emptyAssociationContext() );
		assertThat( actualAssociation.get( rowKey ).hashCode() ).isNotNull();
	}

	private EntityKey createEntityKey(String tableName, String[] columnNames, Object[] values) {
		return new EntityKey( new EntityKeyMetadata( tableName, columnNames ), values );
	}

	private AssociationKey createAssociationKey(EntityKey ownerEntityKey, String collectionRole, String tableName, String[] columnNames, Object[] columnValues, String[] rowKeyColumnNames) {
		AssociationKeyMetadata associationKeyMetadata = new AssociationKeyMetadata.Builder()
			.table( tableName )
			.columnNames( columnNames )
			.rowKeyColumnNames( rowKeyColumnNames )
			.associatedEntityKeyMetadata( new AssociatedEntityKeyMetadata( EMPTY_STRING_ARRAY, null ) )
			.inverse( false )
			.collectionRole( collectionRole )
			.associationKind( AssociationKind.ASSOCIATION )
			.oneToOne( false )
			.build();

		return new AssociationKey( associationKeyMetadata, columnValues, ownerEntityKey );
	}

	private void createDataStoreProvider() throws Exception {
		Properties properties = new Properties();
		properties.putAll( environmentProperties() );
		properties.load( CouchDBDialectTest.class.getClassLoader().getResourceAsStream( "hibernate.properties" ) );
		datastoreProvider.configure( properties );
		datastoreProvider.start();
	}
}
