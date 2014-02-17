/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect.ehcache;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.ogm.datastore.ehcache.EhcacheProperties;
import org.hibernate.ogm.test.utils.TestForIssue;
import org.hibernate.ogm.test.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.ogm.test.utils.jpa.JpaTestCase;
import org.junit.Test;

/**
 * Test for reading property values and association rows back from the Ehcache disk store. This is implicitly ensured by
 * using a cache which allows only one element on the heap and then flows over to disk.
 *
 * @author Gunnar Morling
 */
@TestForIssue(jiraKey = "OGM-443")
public class ReadingFromDiskStoreTest extends JpaTestCase {

	@Test
	public void shouldRetainPropertyValuesWhenReadingFromDiskStore() throws Exception {
		List<Engineer> bixbyEngineers = Arrays.asList(
				new Engineer( "Bob the constructor" ),
				new Engineer( "Biff the destructor" )
		);

		Bridge bixbyCreek = new Bridge( 1L, "Bixby Creek Bridge", bixbyEngineers );

		boolean operationSuccessful = false;
		getTransactionManager().begin();

		final EntityManager em = getFactory().createEntityManager();

		try {
			em.persist( bixbyCreek );
			operationSuccessful = true;
		}
		finally {
			commitOrRollback( operationSuccessful );
		}

		em.clear();
		getTransactionManager().begin();
		operationSuccessful = false;
		try {
			Bridge news = em.find( Bridge.class, 1L );
			assertThat( news ).isNotNull();
			assertThat( news.getName() ).isEqualTo( "Bixby Creek Bridge" );

			em.remove( news );
			assertThat( em.find( Bridge.class, 1L ) ).isNull();
		}
		finally {
			commitOrRollback( operationSuccessful );
		}

		em.close();
	}

	@Test
	public void shouldRetrieveAssociationRowsWhenReadingAssociationFromDisk() throws Exception {
		Engineer bob = new Engineer( "Bob the constructor" );
		Engineer biff = new Engineer( "Biff the destructor" );

		List<Engineer> bixbyEngineers = Arrays.asList( bob, biff );

		Bridge bixbyCreek = new Bridge( 2L, "Bixby Creek Bridge", bixbyEngineers );

		Engineer bruce = new Engineer( "Bruce the initializer" );
		List<Engineer> astoriaEngineers = Arrays.asList( bruce );

		Bridge astoriaMegler = new Bridge( 3L, "Astoria-Megler Bridge", astoriaEngineers );

		boolean operationSuccessful = false;
		getTransactionManager().begin();

		final EntityManager em = getFactory().createEntityManager();

		try {
			em.persist( bixbyCreek );
			em.persist( astoriaMegler );
			operationSuccessful = true;
		}
		finally {
			commitOrRollback( operationSuccessful );
		}

		em.clear();
		getTransactionManager().begin();
		operationSuccessful = false;

		try {
			Bridge loadedBridge = em.find( Bridge.class, 3L );
			assertThat( loadedBridge ).isNotNull();
			assertThat( loadedBridge.getEngineers() ).onProperty( "name" ).containsOnly( "Bruce the initializer" );
			em.remove( loadedBridge );
			assertThat( em.find( Bridge.class, 3L ) ).isNull();

			loadedBridge = em.find( Bridge.class, 2L );
			assertThat( loadedBridge ).isNotNull();
			assertThat( loadedBridge.getEngineers() ).onProperty( "name" ).containsOnly( "Bob the constructor", "Biff the destructor" );
			em.remove( loadedBridge );
			assertThat( em.find( Bridge.class, 2L ) ).isNull();

		}
		finally {
			commitOrRollback( operationSuccessful );
		}
		em.close();
	}

	@Override
	protected void refineInfo(GetterPersistenceUnitInfo info) {
		info.getProperties().put( EhcacheProperties.CONFIGURATION_RESOURCE_NAME, "enforced-disk-read-ehcache.xml" );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { Bridge.class, Engineer.class };
	}
}
