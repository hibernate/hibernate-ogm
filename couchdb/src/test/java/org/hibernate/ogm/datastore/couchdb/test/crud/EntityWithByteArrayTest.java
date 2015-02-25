/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.test.crud;

import java.util.Arrays;
import java.util.Random;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Hardy Ferentschik
 */
public class EntityWithByteArrayTest extends OgmTestCase {
	private Session testSession;
	private byte[] testData;

	@Before
	public void setUp() {
		super.setUp();
		testSession = openSession();

		testData = new byte[20];
		new Random().nextBytes( testData );

	}

	@Test
	@TestForIssue(jiraKey = "OGM-735")
	public void testEntityWithByteArrayCanBePersistedAndLoaded() {
		Transaction transaction = testSession.beginTransaction();
		Foo foo = new Foo( testData );
		testSession.persist( foo );
		transaction.commit();

		testSession.clear();

		transaction = testSession.beginTransaction();
		Foo loadedFoo = (Foo) testSession.get( Foo.class, foo.getId() );
		assertNotNull( "Cannot load persisted object", loadedFoo );
		assertArrayEquals( "Original and loaded data do not match!", testData, loadedFoo.getData() );
		transaction.commit();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Foo.class };
	}

	@Entity
	public static class Foo {
		@Id
		@GeneratedValue(generator = "uuid")
		@GenericGenerator(name = "uuid", strategy = "uuid2")
		private String id;

		private byte[] data;

		public Foo() {
		}

		public Foo(byte[] data) {
			this.data = data;
		}

		public String getId() {
			return id;
		}

		public byte[] getData() {
			return data;
		}

		@Override
		public String toString() {
			return "Foo{" +
					"id=" + id +
					", data=" + Arrays.toString( data ) +
					'}';
		}
	}
}


