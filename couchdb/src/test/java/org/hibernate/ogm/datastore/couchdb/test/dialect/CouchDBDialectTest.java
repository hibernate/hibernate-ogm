/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.test.dialect;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hibernate.ogm.datastore.couchdb.utils.CouchDBTestHelper.environmentProperties;
import static org.hibernate.ogm.datastore.couchdb.utils.CouchDBTestHelper.initEnvironmentProperties;
import static org.hibernate.ogm.util.impl.ArrayHelper.EMPTY_STRING_ARRAY;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.ogm.datastore.couchdb.CouchDBDialect;
import org.hibernate.ogm.datastore.couchdb.dialect.model.impl.CouchDBTupleSnapshot;
import org.hibernate.ogm.datastore.couchdb.impl.CouchDBDatastoreProvider;
import org.hibernate.ogm.datastore.couchdb.utils.CouchDBTestHelper;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.AssociationKind;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.options.navigation.impl.OptionsContextImpl;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSource;
import org.hibernate.ogm.utils.EmptyOptionsContext;
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
		assertThat( actualIdValue, is( 36 ) );
	}

	@Test
	public void getTupleShouldReturnTheSearchedOne() {
		EntityKey key = createEntityKey( "user", new String[] { "id", "age" }, new Object[] { "17", 36 } );
		Tuple createdTuple = dialect.createTuple( key, emptyTupleContext() );

		dialect.updateTuple( createdTuple, key, emptyTupleContext() );

		Tuple actualTuple = dialect.getTuple( key, emptyTupleContext() );

		assertThat( actualTuple.get( "id" ), is( createdTuple.get( "id" ) ) );
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

		dialect.updateTuple( createdTuple, key, emptyTupleContext() );

		Tuple tuple = dialect.getTuple( key, emptyTupleContext() );
		assertThat( (String) tuple.get( "name" ), is( "and" ) );
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

		assertThat( createAssociation.getSnapshot(), notNullValue() );
		assertThat( createAssociation.getSnapshot().getRowKeys().isEmpty(), is( true ) );
	}

	@Test
	public void updateAnAssociationShouldAddATuple() {
		String tableName = "user_address";
		String[] rowKeyColumnNames = new String[] { "user_id", "addresses_id" };
		Object[] rowKeyColumnValues = new Object[] { "Emmanuel", 1 };
		EntityKey entityKey = createEntityKey( "user", new String[] { "id", "age" }, new Object[] { "17", 36 } );
		Tuple tuple = dialect.createTuple( entityKey, emptyTupleContext() );
		dialect.updateTuple( tuple, entityKey, emptyTupleContext() );

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
		dialect.updateAssociation( createAssociation, key, emptyAssociationContext() );

		Association actualAssociation = dialect.getAssociation( key, emptyAssociationContext() );
		assertThat( actualAssociation.get( rowKey ).hashCode(), notNullValue() );
	}

	private EntityKey createEntityKey(String tableName, String[] columnNames, Object[] values) {
		return new EntityKey( new EntityKeyMetadata( tableName, columnNames ), values );
	}

	private AssociationKey createAssociationKey(EntityKey ownerEntityKey, String collectionRole, String tableName, String[] columnNames, Object[] columnValues, String[] rowKeyColumnNames) {
		AssociationKeyMetadata associationKeyMetadata = new AssociationKeyMetadata(
				tableName, columnNames, rowKeyColumnNames, EMPTY_STRING_ARRAY, new AssociatedEntityKeyMetadata( EMPTY_STRING_ARRAY, null ), false
		);

		return new AssociationKey( associationKeyMetadata, columnValues, collectionRole, ownerEntityKey, AssociationKind.ASSOCIATION );
	}

	private void createDataStoreProvider() throws Exception {
		Properties properties = new Properties();
		properties.putAll( environmentProperties() );
		properties.load( CouchDBDialectTest.class.getClassLoader().getResourceAsStream( "hibernate.properties" ) );
		datastoreProvider.configure( properties );
		datastoreProvider.start();
	}

	private AssociationContext emptyAssociationContext() {
		return new AssociationContext(
				OptionsContextImpl.forProperty( Collections.<OptionValueSource>emptyList(), Object.class, "" ),
				null,
				null
		);
	}

	private TupleContext emptyTupleContext() {
		return new TupleContext(
				Collections.<String>emptyList(),
				Collections.<String, AssociatedEntityKeyMetadata>emptyMap(),
				Collections.<String, String>emptyMap(),
				EmptyOptionsContext.INSTANCE
		);
	}
}
