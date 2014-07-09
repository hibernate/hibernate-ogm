/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.options;

import org.fest.assertions.Assertions;
import org.hibernate.ogm.options.spi.Option;
import org.junit.Test;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class OptionTest {

	private static class FirstOption extends Option<String, Boolean> {
		@Override
		public String getOptionIdentifier() {
			return "id1";
		}
	}

	private static class SecondOption extends FirstOption {
		@Override
		public String getOptionIdentifier() {
			return "id2";
		}
	}

	@Test
	public void testEqualsForDifferentOptions() throws Exception {
		FirstOption option1 = new FirstOption();
		SecondOption option2 = new SecondOption();

		Assertions.assertThat( option1 ).isNotEqualTo( option2 );
	}

	public void testHashCodeForDifferentOptions() throws Exception {
		FirstOption option1 = new FirstOption();
		FirstOption option2 = new FirstOption();

		Assertions.assertThat( option1.hashCode() ).isEqualTo( option2.hashCode() );
	}

}
