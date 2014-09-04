/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.descriptor;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.type.impl.StringDateTypeDescriptor;
import org.junit.Test;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class StringDateTypeDescriptorTest {

	@Test
	public void testDescriptorName() throws Exception {
		assertThat( StringDateTypeDescriptor.INSTANCE.getName() ).as( StringDateTypeDescriptor.class.getSimpleName() )
				.isEqualTo( "string_date" );
	}

	@Test
	public void testColumnSpanForNull() throws Exception {
		assertThat( StringDateTypeDescriptor.INSTANCE.getColumnSpan( null ) ).as( "Column span for null" )
				.isEqualTo( 1 );
	}
}
