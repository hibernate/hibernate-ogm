/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.couchdb.test.dialect;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hibernate.ogm.datastore.couchdb.utils.CouchDBTestHelper.environmentProperties;
import static org.hibernate.ogm.datastore.couchdb.utils.CouchDBTestHelper.initEnvironmentProperties;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.ogm.datastore.couchdb.CouchDBDialect;
import org.hibernate.ogm.datastore.couchdb.dialect.model.impl.CouchDBTupleSnapshot;
import org.hibernate.ogm.datastore.couchdb.impl.CouchDBDatastoreProvider;
import org.hibernate.ogm.datastore.couchdb.utils.CouchDBTestHelper;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.AssociationTypeContext;
import org.hibernate.ogm.datastore.spi.SessionContext;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.datastore.spi.TupleTypeContext;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.AssociationKeyMetadata;
import org.hibernate.ogm.grid.AssociationKind;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.utils.EmptyOptionsContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Boriero <dreborier@gmail.com/>
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

		RowKey rowKey = createRowKey( tableName, rowKeyColumnNames, rowKeyColumnValues );
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
				tableName, columnNames, rowKeyColumnNames, AssociationKind.ASSOCIATION, collectionRole, false
		);

		return new AssociationKey( associationKeyMetadata, columnValues, ownerEntityKey );
	}

	private RowKey createRowKey(String tableName, String[] rowKeyColumnNames, Object[] rowKeyColumnValues) {
		return new RowKey( tableName, rowKeyColumnNames, rowKeyColumnValues );
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
				new AssociationTypeContext( EmptyOptionsContext.INSTANCE ),
				new SessionContext()
		);
	}

	private TupleContext emptyTupleContext() {
		return new TupleContext(
				new TupleTypeContext(
						Collections.<String>emptyList(),
						EmptyOptionsContext.INSTANCE,
						Collections.<AssociationKeyMetadata>emptyList()
				),
				new SessionContext()
		);
	}
}
