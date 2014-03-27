/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
