/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.dialectinvocations;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.backendtck.associations.onetoone.Husband;
import org.hibernate.ogm.backendtck.associations.onetoone.Wife;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.dialect.batch.spi.BatchableGridDialect;
import org.hibernate.ogm.dialect.impl.GridDialects;
import org.hibernate.ogm.dialect.spi.DuplicateInsertPreventionStrategy;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.InvokedOperationsLoggingDialect;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestForIssue;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Guillaume Smet
 */
@SkipByGridDialect(value = { GridDialectType.CASSANDRA, GridDialectType.NEO4J_REMOTE, GridDialectType.NEO4J_EMBEDDED, GridDialectType.REDIS_HASH, GridDialectType.INFINISPAN_REMOTE  },
		comment = "For Cassandra and Neo4j, the getAssociation always return an association, thus we don't have the createAssociation call. " +
					"Redis Hash is just weird. Infinispan Remote needs to be investigated.")
@TestForIssue(jiraKey = "OGM-1152")
public class GridDialectOperationInvocationForOneToOneTest extends OgmTestCase {

	@Before
	public void resetOperationsLog() {
		getOperationsLogger().reset();
	}

	@Test
	public void testBidirectionalOneToOne() throws Exception {
		GridDialect gridDialect = getSessionFactory().getServiceRegistry().getService( GridDialect.class );

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

		if ( GridDialects.hasFacet( gridDialect, BatchableGridDialect.class ) ) {
			assertThat( getOperations() ).containsExactly(
					"getTuple", // when adding Husband, ORM looks at Wife and checks if it is transient
								// since it is transient and id is manually set, this leads to a lookup
					"createTuple", // creating Husband tuple
					"createTuple", // creating Wife tuple
					"getTuple", // read Husband as we mightRequireInverseAssociationManagement (OgmEntityPersister:1137)
								// or if non atomic optimistic locking: in this case we could still use the entity entry
								// state cache
								// if the value has been loaded during the overall flush
					"getAssociation", // read the association info from Wife to Husband
										// before that, executes the batch containing the 2 insertOrUpdateTuple operations
					"createAssociation", // could not find the association so create one
					"executeBatch[insertOrUpdateAssociation]" // execute the batch of insert/update operations
			);
		}
		else if ( isDuplicateInsertPreventionStragetyNative( gridDialect ) ) {
			assertThat( getOperations() ).containsExactly(
					"getTuple", // when adding Husband, ORM looks at Wife and checks if it is transient
								// since it is transient and id is manually set, this leads to a lookup
					"createTuple", // creating Husband tuple
					"insertOrUpdateTuple", // inserting Husband without fk
					"createTuple", // creating Wife tuple
					"insertOrUpdateTuple", // inserting Wife without association
					"getTuple", // read Husband as we mightRequireInverseAssociationManagement (OgmEntityPersister:1137)
								// or if non atomic optimistic locking: in this case we could still use the entity entry
								// state cache
								// if the value has been loaded during the overall flush
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
					"getTuple", // read Husband as we mightRequireInverseAssociationManagement (OgmEntityPersister:1137)
								// or if non atomic optimistic locking: in this case we could still use the entity entry
								// state cache
								// if the value has been loaded during the overall flush
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

	private boolean isDuplicateInsertPreventionStragetyNative(GridDialect gridDialect) {
		return DuplicateInsertPreventionStrategy.NATIVE
				.equals( gridDialect.getDuplicateInsertPreventionStrategy( new DefaultEntityKeyMetadata( "TableName", new String[]{ "id" } ) ) );
	}
}
