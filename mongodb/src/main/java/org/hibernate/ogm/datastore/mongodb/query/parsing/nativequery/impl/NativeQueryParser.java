/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.nativequery.impl;

import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor.Operation;
import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.annotations.SuppressSubnodes;

import com.mongodb.util.JSON;

/**
 * A parser for MongoDB queries which can be given in one of the following representations:
 * <ul>
 * <li>Criteria-only find query, e.g. <code>{ $and: [ { name : 'Portia' }, { author : 'Oscar Wilde' } ] }</code>. It is
 * left to MongoDB's own {@link JSON} parser to interpret such queries.
 * <li>As "invocation" of the MongoDB shell API, e.g.
 * <code>db.WILDE_POEM.find({ '$query' : { 'name' : 'Athanasia' }, '$orderby' : { 'name' : 1 } })</code>. Currently the
 * following API methods are supported:
 * <ul>
 * <li>find(criteria)</li>
 * <li>find(criteria, projection)</li>
 * </ul>
 * The parameter values must be given as JSON objects adhering to the <a
 * href="http://docs.mongodb.org/manual/reference/mongodb-extended-json/">strict mode</a> of MongoDB's JSON handling,
 * with the relaxation that Strings may not only be given in double quotes but also single quotes to facilitate their
 * specification in Java Strings.</li>
 * </ul>
 *
 * @author Gunnar Morling
 */
@BuildParseTree
public class NativeQueryParser extends BaseParser<MongoDBQueryDescriptorBuilder> {

	final MongoDBQueryDescriptorBuilder builder;

	public NativeQueryParser() {
		this.builder = new MongoDBQueryDescriptorBuilder();
	}

	public Rule Query() {
		return Sequence( FirstOf( FindQuery(), CriteriaOnlyFindQuery() ), EOI, push( builder ) );
	}

	public Rule FindQuery() {
		return Sequence( Db(), Separator(), Collection(), Separator(), Operation() );
	}

	/**
	 * A find query only given as criterion. Leave it to MongoDB's own parser to handle it.
	 */
	public Rule CriteriaOnlyFindQuery() {
		return Sequence( ZeroOrMore( ANY ), builder.setOperation( Operation.FIND ), builder.setCriteria( match() ) );
	}

	@SuppressNode
	public Rule Db() {
		return Sequence( ZeroOrMore( WhiteSpace() ), "db " );
	}

	@SuppressSubnodes
	public Rule Collection() {
		return Sequence( OneOrMore( TestNot( Separator() ), ANY ), builder.setCollection( match() ) );
	}

	@SuppressNode
	public Rule Separator() {
		return Sequence( ZeroOrMore( WhiteSpace() ), ". " );
	}

	public Rule Operation() {
		return FirstOf(
				Sequence( Find(), builder.setOperation( Operation.FIND ) ),
				Sequence( Count(), builder.setOperation( Operation.COUNT ) )
		);
	}

	public Rule Find() {
		return Sequence(
				"find ",
				"( ",
				Json(), builder.setCriteria( match() ),
				Optional( Sequence( ", ", Json(), builder.setProjection( match() ) ) ),
				") "
		);
	}

	public Rule Count() {
		return Sequence(
				"count ",
				"( ",
				Optional( Sequence( Json(), builder.setCriteria( match() ) ) ),
				") "
		);
	}

	public Rule Json() {
		return FirstOf( JsonObject(), JsonArray() );
	}

	public Rule JsonObject() {
		return Sequence(
				ZeroOrMore( WhiteSpace() ).skipNode(),
				"{ ",
				FirstOf(
						Sequence( Pair(), ZeroOrMore( Sequence( ", ", Pair() ) ) ),
						Optional( Pair() )
				).suppressNode(),
				"} "
				);
	}

	public Rule Pair() {
		return Sequence( JsonString(), ": ", Value() );
	}

	public Rule Value() {
		return FirstOf( JsonString(), JsonNumber(), JsonObject(), JsonArray(), "true ", "false ", "null " );
	}

	@SuppressNode
	public Rule JsonNumber() {
		return Sequence( Integer(), Optional( Sequence( Frac(), Optional( Exp() ) ) ), ZeroOrMore( WhiteSpace() ) );
	}

	public Rule JsonArray() {
		return Sequence(
				"[ ",
				FirstOf(
						Sequence( Value(), ZeroOrMore( Sequence( ", ", Value() ) ) ),
						Optional( Value() )
				),
				"] "
		);
	}

	@SuppressSubnodes
	public Rule JsonString() {
		return FirstOf( JsonDoubleQuotedString(), JsonSingleQuotedString() );
	};

	@SuppressSubnodes
	public Rule JsonDoubleQuotedString() {
		return Sequence( "\"", ZeroOrMore( Character() ), "\" " );
	};

	@SuppressSubnodes
	public Rule JsonSingleQuotedString() {
		return Sequence( "'", ZeroOrMore( SingleQuotedStringCharacter() ), "' " );
	};

	public Rule Character() {
		return FirstOf( EscapedChar(), NormalChar() );
	}

	public Rule SingleQuotedStringCharacter() {
		return FirstOf( SingleQuotedStringEscapedChar(), SingleQuotedStringNormalChar() );
	}

	public Rule EscapedChar() {
		return Sequence( "\\", FirstOf( AnyOf( "\"\\/bfnrt" ), Unicode() ) );
	}

	public Rule SingleQuotedStringEscapedChar() {
		return Sequence( "\\", FirstOf( AnyOf( "'\\/bfnrt" ), Unicode() ) );
	}

	public Rule NormalChar() {
		return Sequence( TestNot( AnyOf( "\"\\") ), ANY );
	}

	public Rule SingleQuotedStringNormalChar() {
		return Sequence( TestNot( AnyOf( "'\\") ), ANY );
	}

	public Rule Unicode() {
		return Sequence( "u", HexDigit(), HexDigit(), HexDigit(), HexDigit() );
	}

	public Rule Integer() {
		return Sequence( Optional( "-" ), NonZeroDigit(), ZeroOrMore( Digit() ) );
	}

	public Rule Digits() {
		return OneOrMore( Digit() );
	}

	public Rule Digit() {
		return CharRange( '0', '9' );
	}

	public Rule NonZeroDigit() {
		return CharRange( '1', '9' );
	}

	public Rule HexDigit() {
		return FirstOf( CharRange( '0', '9' ), CharRange( 'a', 'f' ), CharRange( 'A', 'Z' ) );
	}

	public Rule Frac() {
		return Sequence( ".", Digits() );
	}

	public Rule Exp() {
		return Sequence( IgnoreCase( "e" ), Optional( AnyOf( "+-" ) ), Digits() );
	}

	@SuppressNode
	public Rule WhiteSpace() {
		return OneOrMore( AnyOf( " \n\r\t\f" ) );
	}

	@Override
	@SuppressNode
	protected Rule fromStringLiteral(java.lang.String string) {
		if ( string.endsWith( " " ) ) {
			return Sequence( string.trim(), Optional( WhiteSpace() ) );
		}
		else {
			return String( string );
		}
	}
}
