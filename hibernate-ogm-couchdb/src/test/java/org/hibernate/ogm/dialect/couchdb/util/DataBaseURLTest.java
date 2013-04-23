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
package org.hibernate.ogm.dialect.couchdb.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
public class DataBaseURLTest {

	@Test
	public void shouldReturnTheCorrectServerURL() throws Exception {
		String expectedServerURL = "http://localhost:5984";

		DataBaseURL dataBaseURL = new DataBaseURL( "localhost", 5984, "databasename" );

		assertThat( dataBaseURL.getServerUrl(), is( expectedServerURL ) );
	}

	@Test
	public void shouldReturnTheCorrectServerName() throws Exception {
		String expectedName = "not_important";

		DataBaseURL dataBaseURL = new DataBaseURL( "localhost", 5984, expectedName );

		assertThat( dataBaseURL.getDataBaseName(), is( expectedName ) );
	}

	@Test
	public void shouldReturnTheCorectURLStringRerpresentation() throws Exception {
		String expectedString = "http://localhost:5984/databaseName";

		DataBaseURL dataBaseURL = new DataBaseURL( "localhost", 5984, "databaseName" );

		assertThat( dataBaseURL.toString(), is( expectedString ) );
	}

}
