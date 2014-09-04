/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.inheritance;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.ogm.jpa.impl.OgmPersisterClassResolver;
import org.hibernate.ogm.persister.impl.SingleTableOgmEntityPersister;
import org.hibernate.ogm.persister.impl.UnionSubclassOgmEntityPersister;
import org.junit.Test;

public class OgmPersisterClassResolverTest {

	final OgmPersisterClassResolver resolver = new OgmPersisterClassResolver();

	@Test
	public void testRootSingleTableStrategy() throws Exception {
		assertThat( resolver.getEntityPersisterClass( rootMetadata( InheritanceType.SINGLE_TABLE ) ) )
				.isEqualTo( SingleTableOgmEntityPersister.class );
	}

	@Test
	public void testSubclassSingleTableStrategy() throws Exception {
		assertThat( resolver.getEntityPersisterClass( rootMetadata( InheritanceType.SINGLE_TABLE ) ) )
				.isEqualTo( SingleTableOgmEntityPersister.class );
	}

	@Test
	public void testRootTablePerClassStrategy() throws Exception {
		assertThat( resolver.getEntityPersisterClass( rootMetadata( InheritanceType.TABLE_PER_CLASS ) ) )
				.isEqualTo( SingleTableOgmEntityPersister.class );
	}

	@Test
	public void testSubclassTablePerClassStrategy() throws Exception {
		assertThat( resolver.getEntityPersisterClass( subclassMetadata( InheritanceType.TABLE_PER_CLASS ) ) )
				.isEqualTo( UnionSubclassOgmEntityPersister.class );
	}

	@Test
	public void testRootJoinedStrategy() throws Exception {
		assertThat( resolver.getEntityPersisterClass( rootMetadata( InheritanceType.JOINED ) ) )
				.isEqualTo( SingleTableOgmEntityPersister.class );
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testSubclassJoinedStrategy() throws Exception {
		resolver.getEntityPersisterClass( subclassMetadata( InheritanceType.JOINED ) );
	}

	@Test
	public void testNoInheritanceStrategy() throws Exception {
		assertThat( resolver.getEntityPersisterClass( rootMetadata( InheritanceType.NO_INHERITANCE ) ) )
				.isEqualTo( SingleTableOgmEntityPersister.class );
	}

	@Test
	public void testPersistentRootSingleTableStrategy() throws Exception {
		RootClass rootClass = new RootClass();
		assertThat( resolver.getEntityPersisterClass( rootClass ) ).isEqualTo( SingleTableOgmEntityPersister.class );
	}

	@Test
	public void testSinglePersistentClassTableStrategy() throws Exception {
		Subclass subclass = new SingleTableSubclass( new RootClass() );
		assertThat( resolver.getEntityPersisterClass( subclass ) ).isEqualTo( SingleTableOgmEntityPersister.class );
	}

	@Test
	public void testTablePerClassPersistentSubclassStrategy() throws Exception {
		Subclass subclass = new UnionSubclass( new RootClass() );
		assertThat( resolver.getEntityPersisterClass( subclass ) ).isEqualTo( UnionSubclassOgmEntityPersister.class );
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testPersistenceClassSubclassJoinedStrategy() throws Exception {
		Subclass subclass = new JoinedSubclass( new RootClass() );
		resolver.getEntityPersisterClass( subclass );
	}

	private EntityBinding rootMetadata(InheritanceType inheritanceType) {
		return new EntityBinding( inheritanceType, null );
	}

	private EntityBinding subclassMetadata(InheritanceType inheritanceType) {
		return new EntityBinding( rootMetadata( inheritanceType ) );
	}
}
