package org.hibernate.ogm.dialect.voldemort;

import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.ogm.datastore.voldemort.impl.VoldemortDatastoreProvider;
import org.hibernate.ogm.dialect.VoldemortDialect;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.test.simpleentity.OgmTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.log.Log;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class VoldemortDialectTest extends OgmTestBase {
	private static final int LOOPS = 2500;
	private static final int THREADS = 10;

	private VoldemortDialect dialect;

	@Before
	public void setUp() {
		setUpServer();
		final VoldemortDatastoreProvider p = new VoldemortDatastoreProvider();
		p.start();
		dialect = new VoldemortDialect( p );
	}

	@Test
	public void testIsThreadSafe() throws InterruptedException {
		final RowKey test = new RowKey( "test", null, null );
		Thread[] threads = new Thread[THREADS];
		for ( int i = 0; i < threads.length; i++ ) {
			threads[i] = new Thread( new Runnable() {
				@Override
				public void run() {
					final IdentifierGeneratorHelper.BigIntegerHolder value = new IdentifierGeneratorHelper.BigIntegerHolder();
					for ( int i = 0; i < LOOPS; i++ ) {
						dialect.nextValue( test, value, 1, 1 );
						// Log.info( "nextValue is: " + value.toString() );
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
	}

	@After
	public void tearDown() {
		stopServer();
	}
}
