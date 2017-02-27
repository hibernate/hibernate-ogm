/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.dialectinvocations;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.backendtck.dialectinvocations.AbstractGridDialectOperationInvocationsTest;
import org.hibernate.ogm.dialect.batch.spi.BatchableGridDialect;
import org.hibernate.ogm.dialect.batch.spi.GroupingByEntityDialect;
import org.hibernate.ogm.dialect.impl.GridDialects;
import org.hibernate.ogm.dialect.query.spi.QueryableGridDialect;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.Test;

/**
 * Asserts the number and order of grid dialect operations.
 * @author Gunnar Morling
 */
@TestForIssue(jiraKey = "OGM-1152")
public class GridDialectOperationInvocationsTest extends AbstractGridDialectOperationInvocationsTest {

	@Test
	public void insertUpdateAndDelete() throws Exception {
		GridDialect gridDialect = getGridDialect();

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

		if ( GridDialects.hasFacet( gridDialect, GroupingByEntityDialect.class ) || GridDialects.hasFacet( gridDialect, BatchableGridDialect.class ) ) {
			if ( isDuplicateInsertPreventionStrategyNative( gridDialect ) ) {
				assertThat( getOperations() ).containsExactly(
						"createTuple",
						"executeBatch[group[insertOrUpdateTuple]]",
						"getTuple",
						"executeBatch[group[insertOrUpdateTuple]]",
						"getTuple",
						"executeBatch[removeTuple]"
				);
			}
			else {
				assertThat( getOperations() ).containsExactly(
						"getTuple",
						"createTuple",
						"executeBatch[group[insertOrUpdateTuple]]",
						"getTuple",
						"executeBatch[group[insertOrUpdateTuple]]",
						"getTuple",
						"executeBatch[removeTuple]"
				);
			}
		}
		else {
			assertThat( getOperations() ).containsExactly(
					"getTuple",
					"createTuple",
					"insertOrUpdateTuple",
					"getTuple",
					"insertOrUpdateTuple",
					"getTuple",
					"removeTuple"
			);
		}
	}

	@Test
	public void insertAndUpdateInSameTransaction() throws Exception {
		GridDialect gridDialect = getGridDialect();

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

		if ( GridDialects.hasFacet( gridDialect, GroupingByEntityDialect.class ) || GridDialects.hasFacet( gridDialect, BatchableGridDialect.class ) ) {
			if ( isDuplicateInsertPreventionStrategyNative( gridDialect ) ) {
				assertThat( getOperations() ).containsExactly(
						"createTuple",
						"executeBatch[group[insertOrUpdateTuple]]",
						"executeBatch[group[insertOrUpdateTuple]]",
						"getTuple",
						"executeBatch[removeTuple]"
				);
			}
			else {
				assertThat( getOperations() ).containsExactly(
						"getTuple",
						"createTuple",
						"executeBatch[group[insertOrUpdateTuple]]",
						"executeBatch[group[insertOrUpdateTuple]]",
						"getTuple",
						"executeBatch[removeTuple]"
				);
			}
		}
		else {
			assertThat( getOperations() ).containsExactly(
					"getTuple",
					"createTuple",
					"insertOrUpdateTuple",
					"insertOrUpdateTuple",
					"getTuple",
					"removeTuple"
			);
		}
	}

	@Test
	public void insertSearchUpdateDelete() throws Exception {
		GridDialect gridDialect = getGridDialect();

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

		if ( GridDialects.hasFacet( gridDialect, GroupingByEntityDialect.class ) || GridDialects.hasFacet( gridDialect, BatchableGridDialect.class ) ) {
			if ( GridDialects.hasFacet( gridDialect, QueryableGridDialect.class ) ) {
				assertThat( getOperations() ).containsExactly(
						"createTuple",
						"executeBatch[group[insertOrUpdateTuple]]",
						"executeBackendQuery",
						"executeBatch[group[insertOrUpdateTuple]]",
						"getTuple",
						"executeBatch[removeTuple]"
				);
			}
			else if ( isDuplicateInsertPreventionStrategyNative( gridDialect ) ) {
				assertThat( getOperations() ).containsExactly(
						"createTuple",
						"executeBatch[group[insertOrUpdateTuple]]",
						"getTuple",
						"executeBatch[group[insertOrUpdateTuple]]",
						"getTuple",
						"executeBatch[removeTuple]"
				);
			}
			else {
				assertThat( getOperations() ).containsExactly(
						"getTuple",
						"createTuple",
						"executeBatch[group[insertOrUpdateTuple]]",
						"getTuple",
						"executeBatch[group[insertOrUpdateTuple]]",
						"getTuple",
						"executeBatch[removeTuple]"
				);
			}
		}
		else {
			assertThat( getOperations() ).containsExactly(
					"getTuple",
					"createTuple",
					"insertOrUpdateTuple",
					"getTuple",
					"insertOrUpdateTuple",
					"getTuple",
					"removeTuple"
			);
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StockItem.class };
	}

}
