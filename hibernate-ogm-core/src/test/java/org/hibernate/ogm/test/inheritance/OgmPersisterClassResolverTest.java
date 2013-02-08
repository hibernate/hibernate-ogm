/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
import org.hibernate.ogm.persister.SingleTableOgmEntityPersister;
import org.hibernate.ogm.persister.UnionSubclassOgmEntityPersister;
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
