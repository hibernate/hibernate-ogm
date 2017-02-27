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

import java.util.Properties;

import org.hibernate.ogm.datastore.couchdb.CouchDBDialect;
import org.hibernate.ogm.datastore.couchdb.impl.CouchDBDatastoreProvider;
import org.hibernate.ogm.datastore.couchdb.utils.CouchDBTestHelper;
import org.hibernate.ogm.model.impl.DefaultAssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultAssociationKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKind;
import org.hibernate.ogm.model.key.spi.AssociationType;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Association;
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
	public void removeTupleShouldDeleteTheCreatedTuple() {
		EntityKey key = createEntityKey( "user", new String[] { "id", "age" }, new Object[] { "17", 36 } );
		dialect.createTuple( key, emptyTupleContext() );

		dialect.removeTuple( key, emptyTupleContext() );

		assertThat( new CouchDBTestHelper().getNumberOfEntities( datastoreProvider.getDataStore() ) ).isEqualTo( 0 );
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

	private EntityKey createEntityKey(String tableName, String[] columnNames, Object[] values) {
		return new EntityKey( new DefaultEntityKeyMetadata( tableName, columnNames ), values );
	}

	private AssociationKey createAssociationKey(EntityKey ownerEntityKey, String collectionRole, String tableName, String[] columnNames, Object[] columnValues, String[] rowKeyColumnNames) {
		AssociationKeyMetadata associationKeyMetadata = new DefaultAssociationKeyMetadata.Builder()
			.table( tableName )
			.columnNames( columnNames )
			.rowKeyColumnNames( rowKeyColumnNames )
			.associatedEntityKeyMetadata( new DefaultAssociatedEntityKeyMetadata( EMPTY_STRING_ARRAY, null ) )
			.inverse( false )
			.collectionRole( collectionRole )
			.associationKind( AssociationKind.ASSOCIATION )
			.associationType( AssociationType.BAG )
			.build();

		return new AssociationKey( associationKeyMetadata, columnValues, ownerEntityKey );
	}

	private void createDataStoreProvider() throws Exception {
		Properties properties = new Properties();
		properties.putAll( environmentProperties() );
		CouchDBTestHelper.loadHibernatePropertiesInto( properties );
		datastoreProvider.configure( properties );
		datastoreProvider.start();
	}
}
