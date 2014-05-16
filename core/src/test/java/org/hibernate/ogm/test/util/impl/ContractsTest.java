/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.util.impl;

import org.hibernate.ogm.util.impl.Contracts;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit test for {@link Contracts}.
 *
 * @author Gunnar Morling
 */
public class ContractsTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void shouldRaiseExceptionOnNullObject() {
		expectedException.expect( IllegalArgumentException.class );
		expectedException.expectMessage( "'FooBar' must not be null" );

		Contracts.assertNotNull( null, "FooBar" );
	}

	@Test
	public void shouldRaiseExceptionOnNullParameter() {
		expectedException.expect( IllegalArgumentException.class );
		expectedException.expectMessage( "Parameter 'FooBar' must not be null" );

		Contracts.assertParameterNotNull( null, "FooBar" );
	}
}
