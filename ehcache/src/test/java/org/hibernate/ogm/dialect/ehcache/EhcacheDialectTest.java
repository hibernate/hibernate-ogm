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
package org.hibernate.ogm.dialect.ehcache;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.ogm.datastore.ehcache.impl.EhcacheDatastoreProvider;
import org.hibernate.ogm.grid.RowKey;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class EhcacheDialectTest {

	private static final int LOOPS = 2500;
	private static final int THREADS = 10;

	private EhcacheDialect dialect;

	@Before
	public void setup() {
		final EhcacheDatastoreProvider datastoreProvider = new EhcacheDatastoreProvider();
		datastoreProvider.configure( new HashMap() );
		datastoreProvider.start();
		dialect = new EhcacheDialect( datastoreProvider );
	}

	@Test
	public void testIsThreadSafe() throws InterruptedException {
		final RowKey test = new RowKey( "test", null, null );
		Thread[] threads = new Thread[THREADS];
		for ( int i = 0; i < threads.length; i++ ) {
			threads[i] = new Thread(
					new Runnable() {
						@Override
						public void run() {
							final IdentifierGeneratorHelper.BigIntegerHolder value
									= new IdentifierGeneratorHelper.BigIntegerHolder();
							for ( int i = 0; i < LOOPS; i++ ) {
								dialect.nextValue( test, value, 1, 1 );
							}
						}
					}
			);
			threads[i].start();
		}
		for ( Thread thread : threads ) {
			thread.join();
		}
		final IdentifierGeneratorHelper.BigIntegerHolder value = new IdentifierGeneratorHelper.BigIntegerHolder();
		dialect.nextValue( test, value, 0, 1 );
		assertThat( value.makeValue().intValue(), equalTo( LOOPS * THREADS ) );
	}
}
