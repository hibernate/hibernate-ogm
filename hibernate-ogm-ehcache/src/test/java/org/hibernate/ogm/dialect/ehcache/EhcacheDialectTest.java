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
