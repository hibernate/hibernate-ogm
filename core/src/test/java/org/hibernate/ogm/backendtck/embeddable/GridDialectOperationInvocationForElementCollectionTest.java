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
package org.hibernate.ogm.backendtck.embeddable;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.backendtck.associations.collection.types.GrandChild;
import org.hibernate.ogm.backendtck.associations.collection.types.GrandMother;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.dialect.impl.GridDialects;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.utils.InvokedOperationsLoggingDialect;
import org.hibernate.ogm.utils.OgmTestCase;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class GridDialectOperationInvocationForElementCollectionTest extends OgmTestCase {

	@Before
	public void resetOperationsLog() {
		getOperationsLogger().reset();
	}

	@Test
	public void testEmbeddableCollectionAsList() throws Exception {
		//insert entity with embedded collection
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

		assertThat( getOperations() ).containsExactly(
				"getTuple",
				"createTuple",
				"insertOrUpdateTuple",
				"getAssociation",
				"createAssociation",
				"insertOrUpdateAssociation"
		);
		session.clear();

		resetOperationsLog();

		//remove one of the elements and add a new one
		tx = session.beginTransaction();
		grandMother = (GrandMother) session.get( GrandMother.class, grandMother.getId() );
		grandMother.getGrandChildren().remove( 0 );
		tx.commit();
		session.clear();

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

		resetOperationsLog();

		//assert removal has been propagated
		tx = session.beginTransaction();
		grandMother = (GrandMother) session.get( GrandMother.class, grandMother.getId() );
		assertThat( grandMother.getGrandChildren() ).onProperty( "name" ).containsExactly( "Leia" );

		session.delete( grandMother );
		tx.commit();

		assertThat( getOperations() ).containsExactly(
				"getTuple", // load grand mother
				"getAssociation", // load collection
				"getAssociation", // load collection for removal
				"removeAssociation", // actual collection removal
				"removeTuple" // remove tuple
				);
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
}
