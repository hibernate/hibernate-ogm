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
package org.hibernate.ogm.test.couchdb.associations;

import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.options.couchdb.AssociationStorageType;
import org.hibernate.ogm.test.utils.CouchDBTestHelper;
import org.hibernate.ogm.test.utils.OgmTestCase;
import org.hibernate.ogm.test.utils.TestHelper;
import org.junit.Before;

/**
 * Base for tests for configuring association storage strategies.
 *
 * @author Gunnar Morling
 */
public abstract class AssociationStorageTestBase extends OgmTestCase {

	protected static CouchDBTestHelper testHelper = new CouchDBTestHelper();

	protected OgmConfiguration configuration;
	protected OgmSessionFactory sessions;

	@Before
	public void setupConfiguration() {
		configuration = TestHelper.getDefaultTestConfiguration( getAnnotatedClasses() );
		configure( configuration );
	}

	protected void setupSessionFactory() {
		sessions = configuration.buildSessionFactory();
	}

	protected int associationDocumentCount() {
		return testHelper.getNumberOfAssociations( AssociationStorageType.ASSOCIATION_DOCUMENT, sessions );
	}

	protected int inEntityAssociationCount() {
		return testHelper.getNumberOfAssociations( AssociationStorageType.IN_ENTITY, sessions );
	}
}
