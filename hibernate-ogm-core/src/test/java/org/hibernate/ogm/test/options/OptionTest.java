/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.test.options;

import org.fest.assertions.Assertions;
import org.hibernate.ogm.options.spi.Option;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class OptionTest {

	private static class FirstOption extends Option<String, FirstOption> {
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
	public void testGetType() {
		FirstOption option1 = new FirstOption();
		Class<FirstOption> optionType = option1.getOptionType();
		Assertions.assertThat( optionType ).isEqualTo( FirstOption.class );
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
