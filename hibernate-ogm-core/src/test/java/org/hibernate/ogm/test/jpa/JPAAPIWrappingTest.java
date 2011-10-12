/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.jpa;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.util.HashMap;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Rule;
import org.junit.Test;

import org.hibernate.ogm.jpa.impl.OgmEntityManager;
import org.hibernate.ogm.jpa.impl.OgmEntityManagerFactory;
import org.hibernate.ogm.test.jpa.util.JpaTestCase;
import org.hibernate.ogm.test.utils.PackagingRule;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class JPAAPIWrappingTest extends JpaTestCase {

	@Rule
	public PackagingRule packaging = new PackagingRule();

	@Test
	public void testWrappedStandalone() throws Exception {
		String fileName = "jtastandalone.jar";
		JavaArchive archive = ShrinkWrap.create( JavaArchive.class, fileName );

		archive.addClass( Poem.class );

		ArchivePath path = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addAsResource( "persistencexml/jpajtastandalone.xml", path );

		File testPackage = new File( packaging.getTargetDir(), fileName );
		archive.as( ZipExporter.class ).exportTo( testPackage, true );

		packaging.addPackageToClasspath( testPackage );

		final EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpajtastandalone" );
		assertThat( emf.getClass() ).isEqualTo( OgmEntityManagerFactory.class );

		EntityManager em = emf.createEntityManager();
		assertThat( em.getClass() ).isEqualTo( OgmEntityManager.class );
		em.close();

		em = emf.createEntityManager( new HashMap() );
		assertThat( em.getClass() ).isEqualTo( OgmEntityManager.class );
		em.close();

		emf.close();
	}

	@Test
	public void testWrapInContainer() throws Exception {
		assertThat( getFactory().getClass() ).isEqualTo( OgmEntityManagerFactory.class );
		EntityManager entityManager = getFactory().createEntityManager();
		assertThat( entityManager.getClass() ).isEqualTo( OgmEntityManager.class );
		entityManager.close();
		entityManager = getFactory().createEntityManager( new HashMap() );
		assertThat( entityManager.getClass() ).isEqualTo( OgmEntityManager.class );
		entityManager.close();
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] {
				Poem.class
		};
	}
}
