/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.test.type.descriptor.java;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.orientechnologies.orient.core.id.ORecordId;
import org.hibernate.ogm.datastore.orientdb.type.descriptor.java.ORecordIdTypeDescriptor;

/**
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class ORecordIdTypeDescriptorTest {

	/**
	 * Test of toString method, of class ORecordIdTypeDescriptor.
	 */
	@Test
	public void testToString() {
		System.out.println( "toString" );
		ORecordId id = new ORecordId( 30, 100L );
		ORecordIdTypeDescriptor instance = new ORecordIdTypeDescriptor();
		String expResult = "#30:100";
		String result = instance.toString( id );
		assertEquals( expResult, result );
	}

	/**
	 * Test of fromString method, of class ORecordIdTypeDescriptor.
	 */
	@Test
	public void testFromString() {
		System.out.println( "fromString" );
		String string = "#30:100";
		ORecordIdTypeDescriptor instance = new ORecordIdTypeDescriptor();
		ORecordId expResult = new ORecordId( 30, 100L );
		ORecordId result = instance.fromString( string );
		assertEquals( expResult, result );
	}

}
