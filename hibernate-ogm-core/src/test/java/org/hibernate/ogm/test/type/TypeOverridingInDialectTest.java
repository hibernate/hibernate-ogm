/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.type;

import org.hibernate.ogm.test.utils.PackagingRule;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Rule;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.RollbackException;
import javax.transaction.TransactionManager;
import java.io.File;
import java.util.Date;
import java.util.UUID;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.test.utils.jpa.JpaTestCase.extractJBossTransactionManager;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class TypeOverridingInDialectTest {
	@Rule
	public PackagingRule packaging = new PackagingRule();

	@Test
	public void testOverriddenTypeInDialect() throws Exception {
		String fileName = "jtastandalone.jar";
		JavaArchive archive = ShrinkWrap.create( JavaArchive.class, fileName );

		archive.addClass( Poem.class );
		archive.addClass( OverridingTypeDialect.class );
		archive.addClass( ExplodingType.class );
		archive.addClass( TypeOverridingInDialectTest.class );

		ArchivePath path = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addAsResource( "persistencexml/jpajtastandalone-customdialect.xml", path );

		File testPackage = new File( PackagingRule.getTargetDir(), fileName );
		archive.as( ZipExporter.class ).exportTo( testPackage, true );

		packaging.addPackageToClasspath( testPackage );

		final EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpajtastandalone" );

		TransactionManager transactionManager = extractJBossTransactionManager( emf );
		transactionManager.begin();
		final EntityManager em = emf.createEntityManager();
		try {
			Poem poem = new Poem();
			poem.setName( "L'albatros" );
			poem.setPoemSocietyId( UUID.randomUUID() );
			poem.setCreation( new Date() );
			em.persist( poem );
			transactionManager.commit();
			assertThat( true ).as( "Custom type not used" ).isFalse();
		}
		catch ( RollbackException e ) {
			//make this chaing more robust
			assertThat( e.getCause().getCause().getMessage() ).isEqualTo( "Exploding type" );
		}
		finally {
			try {
				transactionManager.rollback();
			}
			catch ( Exception e ) {
				//we try
			}
			em.close();
			emf.close();
		}
	}

}
