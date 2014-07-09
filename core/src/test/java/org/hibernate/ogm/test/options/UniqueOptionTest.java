/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.options;

import org.fest.assertions.Assertions;
import org.hibernate.ogm.options.spi.UniqueOption;
import org.junit.Test;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class UniqueOptionTest {

	private static class FirstOption extends UniqueOption {
	}

	private static class SecondOption extends UniqueOption {
	}

	@Test
	public void testEqualsForDifferentUniqueOptions() throws Exception {
		FirstOption option1 = new FirstOption();
		SecondOption option2 = new SecondOption();

		Assertions.assertThat( option1 ).isNotEqualTo( option2 );
	}

	@Test
	public void testEqualsForTwoInstancesOfTheSameUniqueOption() throws Exception {
		FirstOption option1 = new FirstOption();
		FirstOption option2 = new FirstOption();

		Assertions.assertThat( option1 ).isEqualTo( option2 );
	}

	@Test
	public void testHashCodeForDifferentUniqueOptions() throws Exception {
		FirstOption option1 = new FirstOption();
		SecondOption option2 = new SecondOption();

		Assertions.assertThat( option1.hashCode() ).isNotEqualTo( option2.hashCode() );
	}

	@Test
	public void testHashCodeForTwoInstancesOfTheSameUniqueOption() throws Exception {
		FirstOption option1 = new FirstOption();
		FirstOption option2 = new FirstOption();

		Assertions.assertThat( option1.hashCode() ).isEqualTo( option2.hashCode() );
	}

}
