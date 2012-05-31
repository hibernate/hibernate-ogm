package org.hibernate.ogm.datastore.voldemort.impl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.ogm.dialect.VoldemortDialect;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.test.simpleentity.OgmTestBase;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import voldemort.versioning.Versioned;

import com.google.gson.Gson;

public class VoldemortImplTest extends OgmTestBase {
	private static final Log log = LoggerFactory.make();
	private static final int LOOPS = 200;
	private static final int THREADS = 10;
	private VoldemortDatastoreProvider provider;
	private VoldemortDialect dialect;
	private final Gson gson = new Gson();

	@Before
	public void setUp() {
		this.setUpServer();
		provider = new VoldemortDatastoreProvider();
		provider.start();
		provider.setFlushToDb( true );
		dialect = new VoldemortDialect( provider );
	}

	@Test
	public void testFlushSequenceToDb() throws InterruptedException {
		final RowKey test = new RowKey( "test", null, null );
		Thread[] threads = new Thread[THREADS];
		for ( int i = 0; i < threads.length; i++ ) {
			threads[i] = new Thread( new Runnable() {
				@Override
				public void run() {
					final IdentifierGeneratorHelper.BigIntegerHolder value = new IdentifierGeneratorHelper.BigIntegerHolder();
					for ( int i = 0; i < LOOPS; i++ ) {
						dialect.nextValue( test, value, 1, 1 );
					}
				}
			} );
			threads[i].start();
		}
		for ( Thread thread : threads ) {
			thread.join();
		}
		final IdentifierGeneratorHelper.BigIntegerHolder value = new IdentifierGeneratorHelper.BigIntegerHolder();
		dialect.nextValue( test, value, 0, 1 );
		assertThat( value.makeValue().intValue(), equalTo( LOOPS * THREADS ) );

		Versioned v = provider.getValue( provider.getVoldemortSequenceStoreName(),
				gson.toJson( test.getRowKeyAsMap() ), true );
		Map<String, Integer> m = (Map<String, Integer>) v.getValue();
		assertThat( (Integer) m.get( "nextSequence" ), equalTo( LOOPS * THREADS ) );
	}

	@Test
	public void testUpdateAction() {
		provider.setUpdateAction( new TestUpdateAction() );
		RowKey rowKeys[] = new RowKey[10];

		for ( int i = 0; i < rowKeys.length; i++ ) {
			rowKeys[i] = new RowKey( "test" + i, null, null );
			final IdentifierGeneratorHelper.BigIntegerHolder value = new IdentifierGeneratorHelper.BigIntegerHolder();
			dialect.nextValue( rowKeys[i], value, 0, 1 );
		}

		for ( int i = 0; i < rowKeys.length; i++ ) {
			assertNull( provider.getValue( provider.getVoldemortSequenceStoreName(),
					gson.toJson( rowKeys[i].getRowKeyAsMap() ), true ) );
		}
	}

	@After
	public void tearDown() {
		this.stopServer();
	}
}
