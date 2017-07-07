/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.dialectinvocations;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.backendtck.associations.collection.types.GrandChild;
import org.hibernate.ogm.backendtck.associations.collection.types.GrandMother;
import org.hibernate.ogm.dialect.batch.spi.BatchableGridDialect;
import org.hibernate.ogm.dialect.batch.spi.GroupingByEntityDialect;
import org.hibernate.ogm.dialect.impl.GridDialects;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.Test;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Guillaume Smet
 */
@SkipByGridDialect(value = { GridDialectType.NEO4J_REMOTE, GridDialectType.NEO4J_EMBEDDED },
		comment = "For Neo4j, the getAssociation always return an association, thus we don't have the createAssociation call. " +
				"Redis Hash is just weird.")
@TestForIssue(jiraKey = "OGM-1152")
public class GridDialectOperationInvocationsForElementCollectionTest extends AbstractGridDialectOperationInvocationsTest {

	@Test
	public void testEmbeddableCollectionAsList() throws Exception {
		GridDialect gridDialect = getGridDialect();

		// Insert entity with embedded collection
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		GrandChild luke = new GrandChild();
		luke.setName( "Luke" );
		GrandChild leia = new GrandChild();
		leia.setName( "Leia" );
		GrandMother grandMother = new GrandMother();
		grandMother.getGrandChildren().add( luke );
		grandMother.getGrandChildren().add( leia );
		session.persist( grandMother );
		tx.commit();

		if ( GridDialects.hasFacet( gridDialect, GroupingByEntityDialect.class ) ) {
			if ( isDuplicateInsertPreventionStrategyNative( gridDialect ) ) {
				assertThat( getOperations() ).containsExactly(
						"createTuple",
						"getAssociation",
						"createAssociation",
						"executeBatch[group[insertOrUpdateTuple,insertOrUpdateAssociation]]"
				);
			}
			else {
				assertThat( getOperations() ).containsExactly(
						"getTuple",
						"createTuple",
						"getAssociation",
						"createAssociation",
						"executeBatch[group[insertOrUpdateTuple,insertOrUpdateAssociation]]"
				);
			}
		}
		else if ( GridDialects.hasFacet( gridDialect, BatchableGridDialect.class ) ) {
			assertThat( getOperations() ).containsExactly(
					"createTuple",
					"getAssociation",
					"executeBatch[group[insertOrUpdateTuple,insertOrUpdateAssociation]]"
			);
		}
		else if ( isDuplicateInsertPreventionStrategyNative( gridDialect ) ) {
			assertThat( getOperations() ).containsExactly(
					"createTuple",
					"insertOrUpdateTuple",
					"getAssociation",
					"createAssociation",
					"insertOrUpdateAssociation"
			);
		}
		else {
			assertThat( getOperations() ).containsExactly(
					"getTuple",
					"createTuple",
					"insertOrUpdateTuple",
					"getAssociation",
					"createAssociation",
					"insertOrUpdateAssociation"
			);
		}

		session.clear();

		resetOperationsLog();

		// Remove one of the elements
		tx = session.beginTransaction();
		grandMother = (GrandMother) session.get( GrandMother.class, grandMother.getId() );
		grandMother.getGrandChildren().remove( 0 );
		tx.commit();
		session.clear();

		if ( GridDialects.hasFacet( gridDialect, GroupingByEntityDialect.class ) || GridDialects.hasFacet( gridDialect, BatchableGridDialect.class ) ) {
			assertThat( getOperations() ).containsExactly(
					"getTuple", // load GrandMother
					"getAssociation", // collection is loaded by gdMother.getGrandChildren()
					"executeBatch[group[insertOrUpdateAssociation,insertOrUpdateAssociation,insertOrUpdateAssociation]]"
			);
		}
		else if ( isDuplicateInsertPreventionStrategyNative( gridDialect ) ) {
			assertThat( getOperations() ).containsExactly(
					"getTuple", // load GrandMother
					"getAssociation", // collection is loaded by gdMother.getGrandChildren()
					"insertOrUpdateAssociation", //remove 1,leia row from association
					"insertOrUpdateAssociation" // put 0,leia (essentially removing luke)
												// the last insertOrUpdateAssociation is skipped as there is no line to insert
			);
		}
		else {
			assertThat( getOperations() ).containsExactly(
					"getTuple", // load GrandMother
					"getAssociation", // collection is loaded by gdMother.getGrandChildren()
					"insertOrUpdateAssociation", //remove 1,leia row from association
					"insertOrUpdateAssociation" // put 0,leia (essentially removing luke)
												// the last insertOrUpdateAssociation is skipped as there is no line to insert
			);
		}

		resetOperationsLog();

		// Assert removal has been propagated
		tx = session.beginTransaction();
		grandMother = (GrandMother) session.get( GrandMother.class, grandMother.getId() );
		assertThat( grandMother.getGrandChildren() ).onProperty( "name" ).containsExactly( "Leia" );

		session.delete( grandMother );
		tx.commit();

		if ( GridDialects.hasFacet( gridDialect, GroupingByEntityDialect.class ) || GridDialects.hasFacet( gridDialect, BatchableGridDialect.class ) ) {
			assertThat( getOperations() ).containsExactly(
					"getTuple", // load grand mother
					"getAssociation", // load collection
					"executeBatch[group[removeAssociation],removeTuple]" // batched removal
			);
		}
		else if ( isDuplicateInsertPreventionStrategyNative( gridDialect ) ) {
			assertThat( getOperations() ).containsExactly(
					"getTuple", // load grand mother
					"getAssociation", // load collection
					"removeAssociation", // actual collection removal
					"removeTuple" // remove tuple
			);
		}
		else {
			assertThat( getOperations() ).containsExactly(
					"getTuple", // load grand mother
					"getAssociation", // load collection
					"removeAssociation", // actual collection removal
					"removeTuple" // remove tuple
			);
		}
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { GrandMother.class, GrandChild.class };
	}

}
