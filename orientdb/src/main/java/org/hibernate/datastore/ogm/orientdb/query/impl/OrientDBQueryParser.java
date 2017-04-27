/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.datastore.ogm.orientdb.query.impl;

import org.hibernate.engine.query.spi.ParameterParser;
import org.parboiled.BaseParser;
import static org.parboiled.BaseParser.ANY;
import static org.parboiled.BaseParser.EOI;
import org.parboiled.Rule;
import org.parboiled.annotations.SkipNode;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.support.StringVar;

/**
 * @author Sergey Chernolyas (sergey.chernolyas@gmail.com)
 */
public class OrientDBQueryParser extends BaseParser<ParameterParser.Recognizer> {

	final ParameterParser.Recognizer journaler;
	final RecognizerAdapter adapter;

	public OrientDBQueryParser(ParameterParser.Recognizer journaler) {
		this.journaler = journaler;
		this.adapter = new RecognizerAdapter( journaler );
	}

	public Rule Query() {
		return Sequence( QueryParts(), EOI, push( journaler ) );
	}

	@SkipNode
	public Rule QueryParts() {
		return ZeroOrMore( QueryPart() );
	}

	@SkipNode
	public Rule QueryPart() {
		return FirstOf( Quoted(), Escaped(), NamedParameter(), Other() );
	}

	@SuppressSubnodes
	public Rule NamedParameter() {
		StringVar name = new StringVar( "" );

		return Sequence(
				ParameterBeginDelimiter(),
				// ZeroOrMore(WhiteSpace()),
				Sequence( OneOrMore( Letter() ), ZeroOrMore( Digit() ) ),
				name.set( match() ),
				// ZeroOrMore(WhiteSpace()),
				// ParameterEndDelimiter(),
				adapter.addNamedParameter( name.get(), currentIndex() ) );
	}

	@SuppressSubnodes
	public Rule Quoted() {
		return Sequence(
				QuoteDelimiter(),
				ZeroOrMore(
						FirstOf(
								EscapedQuoteDelimiter(),
								Sequence( TestNot( QuoteDelimiter() ), ANY ) ) ),
				QuoteDelimiter() );
	}

	@SuppressSubnodes
	public Rule Escaped() {
		return Sequence(
				EscapeDelimiter(),
				ZeroOrMore(
						FirstOf(
								EscapedEscapeDelimiter(),
								Sequence( TestNot( EscapeDelimiter() ), ANY ) ) ),
				EscapeDelimiter() );
	}

	@SuppressSubnodes
	public Rule Other() {
		return OneOrMore( TestNot( NamedParameter() ), TestNot( Quoted() ), TestNot( Escaped() ), ANY );
	}

	public Rule QuoteDelimiter() {
		return Ch( '\'' );
	}

	public Rule EscapedQuoteDelimiter() {
		return Sequence( Ch( '\\' ), Ch( '\'' ) );
	}

	public Rule EscapeDelimiter() {
		return Ch( '`' );
	}

	public Rule EscapedEscapeDelimiter() {
		return Sequence( Ch( '\\' ), Ch( '`' ) );
	}

	public Rule ParameterBeginDelimiter() {
		return Ch( ':' );
	}

	/*
	 * public Rule ParameterEndDelimiter() { return Ch('}'); }
	 */
	public Rule Alphanumeric() {
		return FirstOf( Letter(), Digit() );
	}

	public Rule Letter() {
		return FirstOf( CharRange( 'a', 'z' ), CharRange( 'A', 'Z' ) );
	}

	public Rule Digit() {
		return CharRange( '0', '9' );
	}

	public Rule WhiteSpace() {
		return OneOrMore( AnyOf( " \t\f" ) );
	}

	/**
	 * Wraps a {@link Recognizer} so it can be invoked by the parser (which requires the methods to return a
	 * {@code boolean}).
	 *
	 * @author Gunnar Morling
	 */
	private static class RecognizerAdapter {

		private final ParameterParser.Recognizer recognizer;

		private RecognizerAdapter(ParameterParser.Recognizer recognizer) {
			this.recognizer = recognizer;
		}

		private boolean addNamedParameter(String name, int position) {
			recognizer.namedParameter( name, position );
			return true;
		}
	}

}
