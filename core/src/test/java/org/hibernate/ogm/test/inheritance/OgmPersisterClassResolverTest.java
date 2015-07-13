/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.inheritance;

import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.ogm.jpa.impl.OgmPersisterClassResolver;
import org.hibernate.ogm.persister.impl.SingleTableOgmEntityPersister;
import org.hibernate.ogm.persister.impl.UnionSubclassOgmEntityPersister;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class OgmPersisterClassResolverTest {

	final OgmPersisterClassResolver resolver = new OgmPersisterClassResolver();

	@Test
	public void testPersistentRootSingleTableStrategy() throws Exception {
		RootClass rootClass = new RootClass( null );
		assertThat( resolver.getEntityPersisterClass( rootClass ) ).isEqualTo( SingleTableOgmEntityPersister.class );
	}

	@Test
	public void testSinglePersistentClassTableStrategy() throws Exception {
		Subclass subclass = new SingleTableSubclass( new RootClass( null ), null );
		assertThat( resolver.getEntityPersisterClass( subclass ) ).isEqualTo( SingleTableOgmEntityPersister.class );
	}

	@Test
	public void testTablePerClassPersistentSubclassStrategy() throws Exception {
		Subclass subclass = new UnionSubclass( new RootClass( null ), null );
		assertThat( resolver.getEntityPersisterClass( subclass ) ).isEqualTo( UnionSubclassOgmEntityPersister.class );
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testPersistenceClassSubclassJoinedStrategy() throws Exception {
		Subclass subclass = new JoinedSubclass( new RootClass( null ), null );
		resolver.getEntityPersisterClass( subclass );
	}
}
