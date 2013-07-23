/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.simpleentity;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.test.utils.TestHelper.assertNumberOfAssociations;
import static org.hibernate.ogm.test.utils.TestHelper.assertNumberOfEntities;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.test.utils.OgmTestRunner;
import org.hibernate.ogm.test.utils.SessionFactoryConfiguration;
import org.hibernate.ogm.test.utils.TestEntities;
import org.hibernate.ogm.test.utils.TestSessionFactory;
import org.junit.runner.RunWith;

/**
 * Base class for OGM tests. While tests also can directly make use of {@link OgmTestRunner}, this base class provides
 * template methods for entity type configuration and modifications to {@link Configuration} as well as a member for the
 * session factory.
 *
 * @author Gunnar Morling
 */
@RunWith(OgmTestRunner.class)
public abstract class OgmTestCase {

	/**
	 * The session factory used by all test methods of a test class.
	 */
	@TestSessionFactory
	protected SessionFactory sessions;

	@TestEntities
	private Class<?>[] getTestEntities() {
		return getAnnotatedClasses();
	}

	/**
	 * Must be implemented by subclasses to return the entity types used by this test.
	 *
	 * @return an array with this tests entity types
	 */
	protected abstract Class<?>[] getAnnotatedClasses();

	@SessionFactoryConfiguration
	private void modifyConfiguration(Configuration cfg) {
		configure( cfg );
	}

	/**
	 * Can be overridden in subclasses to inspect or modifcy the {@link Configuration} of this test.
	 *
	 * @param cfg the configuration
	 */
	protected void configure(Configuration cfg) {
	}

	protected Session openSession() {
		return sessions.openSession();
	}

	protected SessionFactoryImplementor sfi() {
		return (SessionFactoryImplementor) sessions;
	}

	protected void checkCleanCache() {
		assertThat( assertNumberOfEntities( 0, sessions ) ).as( "Entity cache should be empty" ).isTrue();
		assertThat( assertNumberOfAssociations( 0, sessions ) ).as( "Association cache should be empty" ).isTrue();
	}
}
