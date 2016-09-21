/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.dialectinvocations;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.dialect.impl.GridDialects;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.utils.InvokedOperationsLoggingDialect;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * Asserts the number and order of grid dialect operations.
 *
 * @author Gunnar Morling
 */
public class GridDialectOperationInvocationsTest extends OgmTestCase {

	@Before
	public void resetOperationsLog() {
		getOperationsLogger().reset();
	}

	@Test
	public void insertUpdateAndDelete() throws Exception {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// insert
		StockItem item = new StockItem();
		item.setId( "item-1" );
		item.setItemName( "Fairway Wood 19°" );
		item.setCount( 25 );
		session.persist( item );

		transaction.commit();
		session.clear();
		transaction = session.beginTransaction();

		// load and update
		StockItem loadedItem = (StockItem) session.get( StockItem.class, item.getId() );
		assertNotNull( "Cannot load persisted object", loadedItem );
		loadedItem.setCount( 24 );

		transaction.commit();
		session.clear();
		transaction = session.beginTransaction();

		// delete
		loadedItem = (StockItem) session.get( StockItem.class, item.getId() );
		assertNotNull( "Cannot load persisted object", loadedItem );
		session.delete( loadedItem );

		transaction.commit();
		session.clear();
		session.close();

		assertThat( getOperations() ).containsExactly(
				"createTuple",
				"executeBatch[group[insertOrUpdateTuple]]",
				"getTuple",
				"executeBatch[group[insertOrUpdateTuple]]",
				"getTuple",
				"executeBatch[removeTuple]"
		);
	}

	@Test
	public void insertGetOfEntityAndElementCollectionAndDelete() throws Exception {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// insert
		Product product = new Product(
				"product-1",
				"Hybrid 25°",
				new Vendor( "Leaveland", 10 ), new Vendor( "Headaway", 9 )
		);
		session.persist( product );

		transaction.commit();
		session.clear();
		transaction = session.beginTransaction();

		// load and update
		Product loadedProduct = (Product) session.get( Product.class, "product-1" );
		assertNotNull( "Cannot load persisted object", loadedProduct );
		assertThat( loadedProduct.getVendors() ).onProperty( "name" ).containsOnly( "Leaveland", "Headaway" );

		transaction.commit();
		session.clear();
		transaction = session.beginTransaction();

		// delete
		loadedProduct = (Product) session.get( Product.class, "product-1" );
		assertNotNull( "Cannot load persisted object", loadedProduct );
		session.delete( loadedProduct );

		transaction.commit();
		session.clear();
		session.close();

		assertThat( getOperations() ).containsExactly(
				"createTuple",
				"getAssociation",
				"executeBatch[group[insertOrUpdateTuple,insertOrUpdateAssociation]]",
				"getTuple",
				"getAssociation",
				"getTuple",
				"getAssociation",
				"executeBatch[group[removeAssociation],removeTuple]"
				);
	}

	@Test
	public void insertAndUpdateInSameTransaction() throws Exception {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// insert
		StockItem item = new StockItem();
		item.setId( "item-1" );
		item.setItemName( "Fairway Wood 19°" );
		item.setCount( 25 );
		session.persist( item );

		session.flush();

		// update
		item.setCount( 24 );

		transaction.commit();
		session.clear();
		transaction = session.beginTransaction();

		StockItem loadedItem = (StockItem) session.get( StockItem.class, item.getId() );
		assertNotNull( "Cannot load persisted object", loadedItem );
		session.delete( loadedItem );

		transaction.commit();
		session.close();

		assertThat( getOperations() ).containsExactly(
				"createTuple",
				"executeBatch[group[insertOrUpdateTuple]]",
				"executeBatch[group[insertOrUpdateTuple]]",
				"getTuple",
				"executeBatch[removeTuple]"
		);
	}

	@Test
	public void insertSearchUpdateDelete() throws Exception {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// insert
		StockItem item = new StockItem();
		item.setId( "item-1" );
		item.setItemName( "Fairway Wood 19°" );
		item.setCount( 25 );
		session.persist( item );

		transaction.commit();
		session.clear();
		transaction = session.beginTransaction();

		// search and update
		StockItem loadedItem = (StockItem) session.createQuery( "from StockItem si" ).uniqueResult();
		assertNotNull( "Cannot load persisted object", loadedItem );
		loadedItem.setCount( 24 );

		transaction.commit();
		session.clear();
		transaction = session.beginTransaction();

		loadedItem = (StockItem) session.get( StockItem.class, item.getId() );
		assertNotNull( "Cannot load persisted object", loadedItem );
		assertThat( loadedItem.getCount() ).isEqualTo( 24 );
		session.delete( loadedItem );

		transaction.commit();
		session.close();
		assertThat( getOperations() ).containsExactly(
				"createTuple",
				"executeBatch[group[insertOrUpdateTuple]]",
				"executeBackendQuery",
				"executeBatch[group[insertOrUpdateTuple]]",
				"getTuple",
				"executeBatch[removeTuple]"
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StockItem.class, Product.class };
	}

	@Override
	protected void configure(Map<String, Object> settings) {
		settings.put( OgmProperties.GRID_DIALECT, InvokedOperationsLoggingDialect.class );
	}

	private InvokedOperationsLoggingDialect getOperationsLogger() {
		GridDialect gridDialect = getSessionFactory().getServiceRegistry().getService( GridDialect.class );
		InvokedOperationsLoggingDialect invocationLogger = GridDialects.getDelegateOrNull(
				gridDialect,
				InvokedOperationsLoggingDialect.class
		);

		return invocationLogger;
	}

	private List<String> getOperations() {
		return getOperationsLogger().getOperations();
	}
}
