package org.hibernate.ogm.datastore.elasticsearch;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.util.impl.ArrayHelper.EMPTY_STRING_ARRAY;
import static org.hibernate.ogm.utils.GridDialectOperationContexts.emptyAssociationContext;
import static org.hibernate.ogm.utils.GridDialectOperationContexts.emptyTupleContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.elasticsearch.common.base.Strings;
import org.elasticsearch.common.collect.Sets;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.elasticsearch.impl.ElasticSearchDatastoreProvider;
import org.hibernate.ogm.datastore.elasticsearch.test.ElasticSearchTestHelper;
import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.model.impl.DefaultAssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultAssociationKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.AssociationKind;
import org.hibernate.ogm.model.spi.Tuple;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class ElasticSearchDialectTest {

	private final ElasticSearchDatastoreProvider datastoreProvider = new ElasticSearchDatastoreProvider();
	private ElasticSearchDialect dialect;

	@BeforeClass
	public static void initEnvironmentProperties() {
		String hostnames = cleanNull(System.getenv("ELASTICSEARCH_HOSTNAMES"));
		if (hostnames != null) {
			System.setProperty(OgmProperties.HOST, hostnames);
		}
	}

	// Maven's surefire plugin set it to the string 'null'
	private static String cleanNull(String value) {
		return Strings.emptyToNull(String.valueOf(value).replace("null", ""));
	}

	@Before
	public void setUp() throws Exception {
		Properties properties = new Properties();
		properties.putAll(environmentProperties());
		properties.load(getClass().getClassLoader().getResourceAsStream("hibernate.properties"));
		datastoreProvider.configure(properties);
		datastoreProvider.start();
		dialect = new ElasticSearchDialect(datastoreProvider);
	}

	public static Map<String, String> environmentProperties() {
		Set<String> keys = Sets.newHashSet(OgmProperties.HOST, OgmProperties.DATABASE, OgmProperties.USERNAME, OgmProperties.PASSWORD, OgmProperties.CREATE_DATABASE);
		Map<String, String> environmentProperties = new HashMap<>(keys.size());
		for (String key : keys) {
			String value = cleanNull(System.getProperty(key));
			if (value != null) {
				environmentProperties.put(key, value);
			}
		}
		return environmentProperties;
	}

	@After
	public void tearDown() throws Exception {
		dialect.dropDatabase();
		datastoreProvider.stop();
	}

	@Test
	public void createTupleShouldReturnANewTuple() {

		EntityKey key = createEntityKey("user", new String[] { "id", "age" }, new Object[] { "17", 36 });
		Tuple createdTuple = dialect.createTuple(key, emptyTupleContext());

		int actualIdValue = (Integer) createdTuple.get("age");
		assertThat(actualIdValue).isEqualTo(36);
	}

	@Test
	public void getTupleShouldReturnTheSearchedOne() {

		EntityKey key = createEntityKey("user", new String[] { "id", "age" }, new Object[] { "17", 36 });
		Tuple createdTuple = dialect.createTuple(key, emptyTupleContext());

		dialect.insertOrUpdateTuple(key, createdTuple, emptyTupleContext());

		Tuple actualTuple = dialect.getTuple(key, emptyTupleContext());

		assertThat(actualTuple.get("id")).isEqualTo(createdTuple.get("id"));
	}

	@Test
	public void removeTupleShouldDeleteTheCreatedTuple() throws Exception {

		EntityKey key = createEntityKey("user", new String[] { "id", "age" }, new Object[] { "17", 36 });
		Tuple tuple = dialect.createTuple(key, emptyTupleContext());
		dialect.insertOrUpdateTuple(key, tuple, emptyTupleContext());
		assertThat(new ElasticSearchTestHelper(datastoreProvider.getClient()).getNumberOfEntities(datastoreProvider.getDatabase(), "user")).isEqualTo(1);

		dialect.removeTuple(key, emptyTupleContext());

		assertThat(new ElasticSearchTestHelper(datastoreProvider.getClient()).getNumberOfEntities(datastoreProvider.getDatabase(), "user")).isEqualTo(0);
	}

	@Test
	public void updateTupleShouldAddTheNewColumnValue() {

		EntityKey key = createEntityKey("user", new String[] { "id", "age" }, new Object[] { "17", 36 });
		Tuple createdTuple = dialect.createTuple(key, emptyTupleContext());
		createdTuple.put("name", "and");

		dialect.insertOrUpdateTuple(key, createdTuple, emptyTupleContext());

		assertThat(new ElasticSearchTestHelper(datastoreProvider.getClient()).getNumberOfEntities(datastoreProvider.getDatabase(), "user")).isEqualTo(1);

		Tuple tuple = dialect.getTuple(key, emptyTupleContext());
		assertThat((String) tuple.get("name")).isEqualTo("and");
	}

	@Ignore
	// association
	@Test
	public void createAssociationShouldCreateAnEmptyAssociation() {

		Object[] columnValues = { "17" };
		String tableName = "user_address";
		String[] columnNames = { "id" };
		String[] rowKeyColumnNames = new String[] { "id" };
		EntityKey entityKey = createEntityKey("user", new String[] { "id", "age" }, new Object[] { "17", 36 });
		String collectionRole = "addresses";

		AssociationKey key = createAssociationKey(entityKey, collectionRole, tableName, columnNames, columnValues, rowKeyColumnNames);

		Association createAssociation = dialect.createAssociation(key, emptyAssociationContext());

		assertThat(createAssociation.getSnapshot()).isNotNull();
		assertThat(createAssociation.getSnapshot().getRowKeys()).isEmpty();
	}

	@Ignore
	// association
	@Test
	public void updateAnAssociationShouldAddATuple() {

		String tableName = "user_address";
		String[] rowKeyColumnNames = new String[] { "user_id", "addresses_id" };
		Object[] rowKeyColumnValues = new Object[] { "Emmanuel", 1 };
		EntityKey entityKey = createEntityKey("user", new String[] { "id", "age" }, new Object[] { "17", 36 });
		Tuple tuple = dialect.createTuple(entityKey, emptyTupleContext());
		dialect.insertOrUpdateTuple(entityKey, tuple, emptyTupleContext());

		AssociationKey key = createAssociationKey(entityKey, "addresses", "user_address", new String[] { "user_id" }, new Object[] { "Emmanuel" }, rowKeyColumnNames);
		Association createAssociation = dialect.createAssociation(key, emptyAssociationContext());

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("user_id", "Emmanuel");
		properties.put("addresses_id", 1);
		Tuple associationTuple = new Tuple(new MapTupleSnapshot(properties));

		RowKey rowKey = new RowKey(rowKeyColumnNames, rowKeyColumnValues);
		createAssociation.put(rowKey, associationTuple);
		dialect.insertOrUpdateAssociation(key, createAssociation, emptyAssociationContext());

		Association actualAssociation = dialect.getAssociation(key, emptyAssociationContext());
		assertThat(actualAssociation.get(rowKey).hashCode()).isNotNull();
	}

	private EntityKey createEntityKey(String tableName, String[] columnNames, Object[] values) {
		return new EntityKey(new DefaultEntityKeyMetadata(tableName, columnNames), values);
	}

	private AssociationKey createAssociationKey(EntityKey ownerEntityKey, String collectionRole, String tableName, String[] columnNames, Object[] columnValues, String[] rowKeyColumnNames) {
		AssociationKeyMetadata associationKeyMetadata = new DefaultAssociationKeyMetadata.Builder().table(tableName).columnNames(columnNames).rowKeyColumnNames(rowKeyColumnNames)
				.associatedEntityKeyMetadata(new DefaultAssociatedEntityKeyMetadata(EMPTY_STRING_ARRAY, null)).inverse(false).collectionRole(collectionRole)
				.associationKind(AssociationKind.ASSOCIATION).oneToOne(false).build();

		return new AssociationKey(associationKeyMetadata, columnValues, ownerEntityKey);
	}

}
