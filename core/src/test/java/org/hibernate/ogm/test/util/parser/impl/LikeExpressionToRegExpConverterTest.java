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
package org.hibernate.ogm.test.util.parser.impl;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.util.parser.impl.LikeExpressionToRegExpConverter;
import org.junit.Test;

/**
 * Unit test for {@link LikeExpressionToRegExpConverter}.
 *
 * @author Gunnar Morling
 */
public class LikeExpressionToRegExpConverterTest {

	@Test
	public void shouldCreateExpressions() {
		LikeExpressionToRegExpConverter converter = new LikeExpressionToRegExpConverter();

		assertThat( converter.getRegExpFromLikeExpression( "foo" ).pattern() ).isEqualTo( "^\\Qfoo\\E$" );
		assertThat( converter.getRegExpFromLikeExpression( "fo_o%bar" ).pattern() ).isEqualTo( "^\\Qfo\\E.\\Qo\\E.*\\Qbar\\E$" );
		assertThat( converter.getRegExpFromLikeExpression( "%foo%" ).pattern() ).isEqualTo( "^.*\\Qfoo\\E.*$" );
		assertThat( converter.getRegExpFromLikeExpression( "%foo[]%bar" ).pattern() ).isEqualTo( "^.*\\Qfoo[]\\E.*\\Qbar\\E$" );
	}

	@Test
	public void shouldCreateExpressionsWithEscapeCharacter() {
		LikeExpressionToRegExpConverter converter = new LikeExpressionToRegExpConverter( '$' );

		assertThat( converter.getRegExpFromLikeExpression( "10$%" ).pattern() ).isEqualTo( "^\\Q10%\\E$" );
		assertThat( converter.getRegExpFromLikeExpression( "10$%, 20$%" ).pattern() ).isEqualTo( "^\\Q10%, 20%\\E$" );
		assertThat( converter.getRegExpFromLikeExpression( "10$%%20$%" ).pattern() ).isEqualTo( "^\\Q10%\\E.*\\Q20%\\E$" );
		assertThat( converter.getRegExpFromLikeExpression( "10$% (in $$)" ).pattern() ).isEqualTo( "^\\Q10% (in $)\\E$" );
	}
}
