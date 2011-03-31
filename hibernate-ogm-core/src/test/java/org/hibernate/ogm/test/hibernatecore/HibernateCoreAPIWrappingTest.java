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
package org.hibernate.ogm.test.hibernatecore;

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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.ejb.HibernateEntityManagerFactory;
import org.hibernate.ogm.hibernatecore.impl.OgmSession;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactory;
import org.hibernate.ogm.jpa.impl.OgmEntityManager;
import org.hibernate.ogm.jpa.impl.OgmEntityManagerFactory;
import org.hibernate.ogm.test.jpa.Poem;
import org.hibernate.ogm.test.jpa.util.JpaTestCase;
import org.hibernate.ogm.test.utils.PackagingRule;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class HibernateCoreAPIWrappingTest extends JpaTestCase {

	@Rule
	public PackagingRule packaging = new PackagingRule();

	@Test
	public void testWrappedFromEntityManagerAPI() throws Exception {
		String fileName = "jtastandalone.jar";
		JavaArchive archive = ShrinkWrap.create( JavaArchive.class, fileName );

		archive.addClass( Contact.class );

		ArchivePath path = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addAsResource( "persistencexml/jpajtastandalone.xml", path );

		File testPackage = new File( packaging.getTargetDir(), fileName );
		archive.as( ZipExporter.class ).exportTo( testPackage, true );

		packaging.addPackageToClasspath( testPackage );

		final EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpajtastandalone" );
		assertThat( HibernateEntityManagerFactory.class.isAssignableFrom( emf.getClass() ) ).isTrue();
		SessionFactory factory = ( (HibernateEntityManagerFactory) emf ).getSessionFactory();
		assertThat( factory.getClass() ).isEqualTo( OgmSessionFactory.class );

		Session s = factory.openSession();
		assertThat( s.getClass() ).isEqualTo( OgmSession.class );
		assertThat( s.getSessionFactory().getClass() ).isEqualTo( OgmSessionFactory.class );
		s.close();

		EntityManager em = emf.createEntityManager();
		assertThat( em.unwrap( Session.class ).getClass() ).isEqualTo( OgmSession.class );
		assertThat( em.getDelegate().getClass() ).isEqualTo( OgmSession.class );

		em.close();

		emf.close();
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] {
				Poem.class
		};
	}
}
