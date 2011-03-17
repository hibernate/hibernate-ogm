package org.hibernate.ogm.test.jpa;

import java.io.File;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.TransactionManager;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Rule;
import org.junit.Test;

import org.hibernate.ogm.test.utils.PackagingRule;
import org.hibernate.transaction.JBossTSStandaloneTransactionManagerLookup;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class JPAStandaloneTest {

	@Rule
	public PackagingRule packaging = new PackagingRule();

	@Test
	public void testJTAStandalone() throws Exception {
		String fileName = "jtastandalone.jar";
		JavaArchive archive = ShrinkWrap.create( JavaArchive.class, fileName );

		archive.addClass( Poem.class );

		ArchivePath path = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addAsResource( "persistencexml/jpajtastandalone.xml", path );

		File testPackage = new File( packaging.getTargetDir(), fileName );
		archive.as( ZipExporter.class ).exportTo( testPackage, true );

		packaging.addPackageToClasspath( testPackage );

		TransactionManager transactionManager = new JBossTSStandaloneTransactionManagerLookup().getTransactionManager( null );

		final EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpajtastandalone" );

		transactionManager.begin();
		final EntityManager em = emf.createEntityManager();
		Poem poem = new Poem();
		poem.setName( "L'albatros" );
		em.persist( poem );
		transactionManager.commit();

		em.clear();

		transactionManager.begin();
		poem = em.find( Poem.class, poem.getId() );
		assertThat( poem ).isNotNull();
		assertThat( poem.getName() ).isEqualTo( "L'albatros" );
		em.remove( poem );
		transactionManager.commit();

		em.close();

		emf.close();
	}


}
