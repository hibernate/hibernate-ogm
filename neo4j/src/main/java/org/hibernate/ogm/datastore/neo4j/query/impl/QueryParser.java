/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.neo4j.query.impl;

import org.hibernate.engine.query.spi.ParameterParser.Recognizer;
import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SkipNode;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.support.StringVar;

@BuildParseTree
public class QueryParser extends BaseParser<Recognizer> {

	final Recognizer journaler;
	final RecognizerAdapter adapter;

	public QueryParser(Recognizer journaler) {
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
		return FirstOf( Quoted(), NamedParameter(), Other() );
	}

	@SuppressSubnodes
	public Rule NamedParameter() {
		StringVar name = new StringVar( "" );

		return Sequence(
			ParameterBeginDelimiter(),
			WhiteSpace(),
			OneOrMore( Alphanumeric() ),
			name.set( match() ),
			WhiteSpace(),
			ParameterEndDelimiter(),
			adapter.addNamedParameter( name.get(), currentIndex() )
		);
	}

	@SuppressSubnodes
	public Rule Quoted() {
		return Sequence(
			QuoteDelimiter(),
			ZeroOrMore(
				FirstOf(
					EscapedQuoteDelimiter(),
					Sequence( TestNot( QuoteDelimiter() ), ANY )
				)
			),
			QuoteDelimiter()
		);
	}

	@SuppressSubnodes
	public Rule Other() {
		return OneOrMore( TestNot( NamedParameter() ), TestNot( Quoted() ), ANY );
	}

	public Rule QuoteDelimiter() {
		return Ch( '\'' );
	}

	public Rule EscapedQuoteDelimiter() {
		return Sequence( Ch( '\\' ), Ch( '\'' ) );
	}

	public Rule ParameterBeginDelimiter() {
		return Ch( '{' );
	}

	public Rule ParameterEndDelimiter() {
		return Ch( '}' );
	}

	public Rule Alphanumeric() {
		return FirstOf( Letter(), Digit() );
	}

	public Rule Letter() {
		return FirstOf( CharRange( 'a', 'z' ), CharRange( 'A', 'Z' ) );
	}

	public Rule Digit() {
		return CharRange( '0', '9' );
	}

	Rule WhiteSpace() {
		return ZeroOrMore( AnyOf( " \t\f" ) );
	}

	private static class RecognizerAdapter {

		private final Recognizer recognizer;

		private RecognizerAdapter(Recognizer recognizer) {
			this.recognizer = recognizer;
		}

		private boolean addNamedParameter(String name, int position) {
			recognizer.namedParameter( name, position );
			return true;
		}
	}
}
