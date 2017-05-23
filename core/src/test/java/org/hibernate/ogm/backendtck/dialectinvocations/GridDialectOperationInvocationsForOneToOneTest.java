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
import org.hibernate.ogm.backendtck.associations.onetoone.Husband;
import org.hibernate.ogm.backendtck.associations.onetoone.Wife;
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
		comment = "For Cassandra and Neo4j, the getAssociation always return an association, thus we don't have the createAssociation call. " +
					"Redis Hash is just weird.")
@TestForIssue(jiraKey = "OGM-1152")
public class GridDialectOperationInvocationsForOneToOneTest extends AbstractGridDialectOperationInvocationsTest {

	@Test
	public void testBidirectionalOneToOne() throws Exception {
		GridDialect gridDialect = getGridDialect();

		final Session session = openSession();
		Transaction transaction = session.beginTransaction();
		Husband husband = new Husband( "alex" );
		husband.setName( "Alex" );
		Wife wife = new Wife( "bea" );
		wife.setName( "Bea" );
		husband.setWife( wife );
		wife.setHusband( husband );
		session.persist( husband );
		session.persist( wife );
		transaction.commit();
		session.clear();

		if ( GridDialects.hasFacet( gridDialect, GroupingByEntityDialect.class ) ) {
			if ( isDuplicateInsertPreventionStrategyNative( gridDialect ) ) {
				assertThat( getOperations() ).containsExactly(
						"getTuple", // when adding Husband, ORM looks at Wife and checks if it is transient
									// since it is transient and id is manually set, this leads to a lookup
						"createTuple", // creating Husband tuple
						"createTuple", // creating Wife tuple
						"getAssociation", // read the association info from Wife to Husband
											// before that, executes the batch containing the 2 insertOrUpdateTuple operations
						"createAssociation", // could not find the association so create one
						"executeBatch[group[insertOrUpdateTuple,insertOrUpdateTuple],group[insertOrUpdateTuple,insertOrUpdateAssociation]]"
						// inserting Husband without FK
						// update Husband with wife FK
						// inserting Wife without association
						// actually update the (inverse) association with a wife -> husband entry
				);
			}
			else {
				assertThat( getOperations() ).containsExactly(
						"getTuple", // when adding Husband, ORM looks at Wife and checks if it is transient
									// since it is transient and id is manually set, this leads to a lookup
						"getTuple", // when inserting Husband, we do a lookup to check whether it is already present
									// DuplicateInsertPreventionStrategy.LOOKUP
						"createTuple", // creating Husband tuple
						"getTuple", // when inserting Wife, we do a lookup to check whether it is already present
									// DuplicateInsertPreventionStrategy.LOOKUP
						"createTuple", // creating Wife tuple
						"getAssociation", // read the association info from Wife to Husband
						"createAssociation", // could not find the association so create one
						"executeBatch[group[insertOrUpdateTuple,insertOrUpdateTuple],group[insertOrUpdateTuple,insertOrUpdateAssociation]]"
						// inserting Husband without FK
						// update Husband with wife FK
						// inserting Wife without association
						// actually update the (inverse) association with a wife -> husband entry
				);
			}
		}
		else if ( GridDialects.hasFacet( gridDialect, BatchableGridDialect.class ) ) {
			assertThat( getOperations() ).containsExactly(
					"getTuple", // when adding Husband, ORM looks at Wife and checks if it is transient
								// since it is transient and id is manually set, this leads to a lookup
					"createTuple", // creating Husband tuple
					"createTuple", // creating Wife tuple
					"getAssociation", // read the association info from Wife to Husband
										// before that, executes the batch containing the 2 insertOrUpdateTuple operations
					"createAssociation", // could not find the association so create one
					"executeBatch[group[insertOrUpdateAssociation]]" // execute the batch of insert/update operations
			);
		}
		else if ( isDuplicateInsertPreventionStrategyNative( gridDialect ) ) {
			assertThat( getOperations() ).containsExactly(
					"getTuple", // when adding Husband, ORM looks at Wife and checks if it is transient
								// since it is transient and id is manually set, this leads to a lookup
					"createTuple", // creating Husband tuple
					"insertOrUpdateTuple", // inserting Husband without fk
					"createTuple", // creating Wife tuple
					"insertOrUpdateTuple", // inserting Wife without association
					"insertOrUpdateTuple", // update Husband with wife FK
					"getAssociation", // read the association info from Wife to Husband
					"createAssociation", // could not find the association so create one
					"insertOrUpdateAssociation" // actually update the (inverse) association with a wife -> husband
												// entry
			);
		}
		else {
			assertThat( getOperations() ).containsExactly(
					"getTuple", // when adding Husband, ORM looks at Wife and checks if it is transient
								// since it is transient and id is manually set, this leads to a lookup
					"getTuple", // when inserting Husband, we do a lookup to check whether it is already present
								// DuplicateInsertPreventionStrategy.LOOKUP
					"createTuple", // creating Husband tuple
					"insertOrUpdateTuple", // inserting Husband without fk
					"getTuple", // when inserting Wife, we do a lookup to check whether it is already present
								// DuplicateInsertPreventionStrategy.LOOKUP
					"createTuple", // creating Wife tuple
					"insertOrUpdateTuple", // inserting Wife without association
					"insertOrUpdateTuple", // update Husband with wife FK
					"getAssociation", // read the association info from Wife to Husband
					"createAssociation", // could not find the association so create one
					"insertOrUpdateAssociation" // actually update the (inverse) association with a wife -> husband
												// entry
			);
		}

		transaction = session.beginTransaction();
		husband = (Husband) session.get( Husband.class, husband.getId() );
		session.clear();
		session.delete( husband );
		session.delete( husband.getWife() );
		transaction.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Husband.class, Wife.class };
	}

}
