/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.test.type;

import org.fest.assertions.Assertions;
import org.hibernate.ogm.datastore.orientdb.type.descriptor.java.ORecordIdTypeDescriptor;
import org.junit.Test;

import com.orientechnologies.orient.core.id.ORecordId;

/**
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class ORecordIdTypeDescriptorTest {

	@Test
	public void testToString() {
		ORecordId id = new ORecordId( 30, 100L );
		String result = new ORecordIdTypeDescriptor().toString( id );

		Assertions.assertThat( result ).isEqualTo( "#30:100" );
	}

	@Test
	public void testFromString() {
		ORecordId expected = new ORecordId( 30, 100L );
		ORecordId result = new ORecordIdTypeDescriptor().fromString( "#30:100" );

		Assertions.assertThat( result ).isEqualTo( expected );
	}

}
