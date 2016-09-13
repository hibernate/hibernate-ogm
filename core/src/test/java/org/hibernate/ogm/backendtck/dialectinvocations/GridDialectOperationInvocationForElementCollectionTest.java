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

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.backendtck.associations.collection.types.GrandChild;
import org.hibernate.ogm.backendtck.associations.collection.types.GrandMother;
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
@SkipByGridDialect(value = { GridDialectType.NEO4J_REMOTE, GridDialectType.NEO4J_EMBEDDED, GridDialectType.REDIS_HASH, GridDialectType.INFINISPAN_REMOTE },
		comment = "For Neo4j, the getAssociation always return an association, thus we don't have the createAssociation call. " +
				"Redis Hash is just weird. Infinispan Remote needs to be investigated.")
@TestForIssue(jiraKey = "OGM-1152")
public class GridDialectOperationInvocationForElementCollectionTest extends OgmTestCase {

	@Before
	public void resetOperationsLog() {
		getOperationsLogger().reset();
	}

	@Test
	public void testEmbeddableCollectionAsList() throws Exception {
		GridDialect gridDialect = getSessionFactory().getServiceRegistry().getService( GridDialect.class );

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

		if ( GridDialects.hasFacet( gridDialect, BatchableGridDialect.class ) ) {
			assertThat( getOperations() ).containsExactly(
					"createTuple",
					"getAssociation",
					"executeBatch[insertOrUpdateTuple,insertOrUpdateAssociation]"
			);
		}
		else if ( isDuplicateInsertPreventionStragetyNative( gridDialect ) ) {
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

		if ( GridDialects.hasFacet( gridDialect, BatchableGridDialect.class ) ) {
			// As mentioned below, the operations give a false impression of optimization
			// as batches are executed under the hood (getAssociation directly calls executeBatch)
			// when getAssociation is called. See the comments.
			assertThat( getOperations() ).containsExactly(
					"getTuple", // load GrandMother
					"getAssociation", // collection is loaded by gdMother.getGrandChildren()
					"getAssociation", // association loaded to delete rows
					"getAssociation", // load association to update row
										// before that, executes the batch containing the insertOrUpdateAssociation operation
					"getAssociation", // load association to insert rows
										// before that, executes the batch containing the insertOrUpdateAssociation operation
					"executeBatch[insertOrUpdateAssociation]" // a no-op in this case since there is no line to update
			);
		}
		else if ( isDuplicateInsertPreventionStragetyNative( gridDialect ) ) {
			assertThat( getOperations() ).containsExactly(
					"getTuple", // load GrandMother
					"getAssociation", // collection is loaded by gdMother.getGrandChildren()
					"getAssociation", // association loaded to delete rows
					"insertOrUpdateAssociation", //remove 1,leia row from association
					"getAssociation", // load association to update row
					"insertOrUpdateAssociation", // put 0,leia (essentially removing luke)
					"getAssociation", // load association to insert rows
					"insertOrUpdateAssociation" // a no-op in this case since there is no line to update
			);
		}
		else {
			assertThat( getOperations() ).containsExactly(
					"getTuple", // load GrandMother
					"getAssociation", // collection is loaded by gdMother.getGrandChildren()
					"getAssociation", // association loaded to delete rows
					"insertOrUpdateAssociation", //remove 1,leia row from association
					"getAssociation", // load association to update row
					"insertOrUpdateAssociation", // put 0,leia (essentially removing luke)
					"getAssociation", // load association to insert rows
					"insertOrUpdateAssociation" // a no-op in this case since there is no line to update
			);
		}

		resetOperationsLog();

		// Assert removal has been propagated
		tx = session.beginTransaction();
		grandMother = (GrandMother) session.get( GrandMother.class, grandMother.getId() );
		assertThat( grandMother.getGrandChildren() ).onProperty( "name" ).containsExactly( "Leia" );

		session.delete( grandMother );
		tx.commit();

		if ( GridDialects.hasFacet( gridDialect, BatchableGridDialect.class ) ) {
			assertThat( getOperations() ).containsExactly(
					"getTuple", // load grand mother
					"getAssociation", // load collection
					"getAssociation", // load collection for removal
					"executeBatch[removeAssociation,removeTuple]" // batched removal
			);
		}
		else if ( isDuplicateInsertPreventionStragetyNative( gridDialect ) ) {
			assertThat( getOperations() ).containsExactly(
					"getTuple", // load grand mother
					"getAssociation", // load collection
					"getAssociation", // load collection for removal
					"removeAssociation", // actual collection removal
					"removeTuple" // remove tuple
			);
		}
		else {
			assertThat( getOperations() ).containsExactly(
					"getTuple", // load grand mother
					"getAssociation", // load collection
					"getAssociation", // load collection for removal
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
				.equals( gridDialect.getDuplicateInsertPreventionStrategy( new DefaultEntityKeyMetadata( "GrandMother", new String[]{ "id" } ) ) );
	}
}
