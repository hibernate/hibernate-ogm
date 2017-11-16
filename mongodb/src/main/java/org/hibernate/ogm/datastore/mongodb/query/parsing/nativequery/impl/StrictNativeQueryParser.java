/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.nativequery.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor;
import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor.Operation;
import org.parboiled.Action;
import org.parboiled.BaseParser;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.buffers.InputBuffer;
import org.parboiled.errors.ParseError;

import com.mongodb.util.JSON;

/**
 * A parser for MongoDB queries which can be given in one of the following representations:
 * <ul>
 * <li>Criteria-only find query, e.g. <code>{ $and: [ { name : 'Portia' }, { author : 'Oscar Wilde' } ] }</code>. It is
 * left to MongoDB's own {@link JSON} parser to interpret such queries.
 * <li>As "invocation" of the MongoDB shell API (CLI), e.g.
 * <code>db.WILDE_POEM.find({ '$query' : { 'name' : 'Athanasia' }, '$orderby' : { 'name' : 1 } })</code>. Currently the
 * following API methods are supported:
 * <ul>
 * <li>find(criteria)</li>
 * <li>find(criteria, projection)</li>
 * <li>findOne(criteria)</li>
 * <li>findOne(criteria, projection)</li>
 * <li>findAndModify(document)</li>
 * <li>insert(document or array)</li>
 * <li>insert(document or array, options)</li>
 * <li>remove(criteria)</li>
 * <li>remove(criteria, options)</li>
 * <li>deleteOne(criteria)</li>
 * <li>deleteOne(criteria, options)</li>
 * <li>deleteMany(criteria)</li>
 * <li>deleteMany(criteria, options)</li>
 * <li>update(criteria, update)</li>
 * <li>update(criteria, update, options)</li>
 * <li>count()</li>
 * <li>count(criteria)</li>
 * <li>aggregate(criteria)</li>
 * <li>distinct(fieldName,criteria,options)</li>
 * <li>mapReduce(mapFunction,reduceFunction,options)</li>
 * </ul>
 * The parameter values must be given as JSON objects adhering to the <a
 * href="http://docs.mongodb.org/manual/reference/mongodb-extended-json/">strict mode</a> of MongoDB's JSON handling,
 * with the relaxation that Strings may not only be given in double quotes but also single quotes to facilitate their
 * specification in Java Strings.</li>
 * </ul>
 *
 * @author Davide D'Alto
 * @author Gunnar Morling
 * @author Thorsten Möller
 * @author Guillaume Smet
 */
@BuildParseTree
public class StrictNativeQueryParser extends BaseParser<MongoDBQueryDescriptor> {

	// TODO There are many more query types than what we support.
	protected static List<String> RESERVED = Arrays.asList(
			"findOne", "findAndModify", "find", "insertOne", "insertMany", "insert",
			"remove", "deleteOne", "deleteMany", "updateOne", "updateMany", "update",
			"replaceOne", "count", "aggregate", "distinct", "mapReduce" );
	protected MongoDBQueryDescriptorBuilder builder = new MongoDBQueryDescriptorBuilder();
	protected CliQueryInfo cliQueryInfo = new CliQueryInfo();

	private static class CliQueryInfo {
		String operationName;
		String collectionName;
		List<String> pathExpression = new ArrayList<>();
		List<String> arguments = new ArrayList<>();
	}
	private Action<MongoDBQueryDescriptor> processPathExpression = context -> {
		List<String> identifiers = cliQueryInfo.pathExpression;
		if ( identifiers.size() <= 2 ) {
			context.getParseErrors().add(
					createParseError(
							context, 2, "CLI query should match 'db.<COLLECTION>.<OPERATION>'"
							)
					);
			return false;
		}
		if ( !"db".equals( identifiers.get( 0 ) ) ) {
			context.getParseErrors().add(
					createParseError(
							context, 2, "First identifier should be 'db'"
							)
					);
			return false;
		}
		cliQueryInfo.operationName = identifiers.get( identifiers.size() - 1 );
		cliQueryInfo.collectionName = String.join( ".", identifiers.subList( 1, identifiers.size() - 1 ) );
		if ( RESERVED.contains( cliQueryInfo.collectionName ) ) {
			context.getParseErrors().add(
					createParseError(
							context,
							identifiers.size(),
							"Collection name should be different from the allowed operations"
							)
					);
			return false;
		}

		return true;
	};

	private Action<MongoDBQueryDescriptor> processCliQueryInfo = context -> {
		builder.setCollection( cliQueryInfo.collectionName );
		switch ( cliQueryInfo.operationName ) {
			case "find": {
				builder.setOperation( Operation.FIND );
				builder.setCriteria( cliQueryInfo.arguments.get( 0 ) );
				if ( cliQueryInfo.arguments.size() > 1 ) {
					builder.setProjection( cliQueryInfo.arguments.get( 1 ) );
				}
				break;
			}
			case "findOne": {
				builder.setOperation( Operation.FINDONE );
				if ( cliQueryInfo.arguments.size() > 0 ) {
					builder.setCriteria( cliQueryInfo.arguments.get( 0 ) );
				}
				if ( cliQueryInfo.arguments.size() > 1 ) {
					builder.setProjection( cliQueryInfo.arguments.get( 1 ) );
				}
				break;
			}
			case "findAndModify": {
				builder.setOperation( Operation.FINDANDMODIFY );
				builder.setCriteria( cliQueryInfo.arguments.get( 0 ) );
				break;
			}
			case "insert": {
				builder.setOperation( Operation.INSERT );
				builder.setUpdateOrInsert( cliQueryInfo.arguments.get( 0 ) );
				if ( cliQueryInfo.arguments.size() > 1 ) {
					builder.setOptions( cliQueryInfo.arguments.get( 1 ) );
				}
				break;
			}
			case "insertOne": {
				builder.setOperation( Operation.INSERTONE );
				builder.setUpdateOrInsert( cliQueryInfo.arguments.get( 0 ) );
				if ( cliQueryInfo.arguments.size() > 1 ) {
					builder.setOptions( cliQueryInfo.arguments.get( 1 ) );
				}
				break;
			}
			case "insertMany": {
				builder.setOperation( Operation.INSERTMANY );
				builder.setUpdateOrInsert( cliQueryInfo.arguments.get( 0 ) );
				if ( cliQueryInfo.arguments.size() > 1 ) {
					builder.setOptions( cliQueryInfo.arguments.get( 1 ) );
				}
				break;
			}
			case "remove": {
				builder.setOperation( Operation.REMOVE );
				builder.setCriteria( cliQueryInfo.arguments.get( 0 ) );
				if ( cliQueryInfo.arguments.size() > 1 ) {
					if ( cliQueryInfo.arguments.get( 1 ).equalsIgnoreCase( "true" ) ) {
						builder.setOptions( "{ 'justOne': " + cliQueryInfo.arguments.get( 1 ) + " }" );
					}
					else {
						builder.setOptions( cliQueryInfo.arguments.get( 1 ) );
					}
				}
				break;
			}
			case "deleteOne": {
				builder.setOperation( Operation.DELETEONE );
				builder.setCriteria( cliQueryInfo.arguments.get( 0 ) );
				if ( cliQueryInfo.arguments.size() > 1 ) {
					builder.setOptions( cliQueryInfo.arguments.get( 1 ) );
				}
				break;
			}
			case "deleteMany": {
				builder.setOperation( Operation.DELETEMANY );
				builder.setCriteria( cliQueryInfo.arguments.get( 0 ) );
				if ( cliQueryInfo.arguments.size() > 1 ) {
					builder.setOptions( cliQueryInfo.arguments.get( 1 ) );
				}
				break;
			}
			case "update": {
				builder.setOperation( Operation.UPDATE );
				builder.setCriteria( cliQueryInfo.arguments.get( 0 ) );
				builder.setUpdateOrInsert( cliQueryInfo.arguments.get( 1 ) );
				if ( cliQueryInfo.arguments.size() > 2 ) {
					builder.setOptions( cliQueryInfo.arguments.get( 2 ) );
				}
				break;
			}
			case "updateOne": {
				builder.setOperation( Operation.UPDATEONE );
				builder.setCriteria( cliQueryInfo.arguments.get( 0 ) );
				builder.setUpdateOrInsert( cliQueryInfo.arguments.get( 1 ) );
				if ( cliQueryInfo.arguments.size() > 2 ) {
					builder.setOptions( cliQueryInfo.arguments.get( 2 ) );
				}
				break;
			}
			case "updateMany": {
				builder.setOperation( Operation.UPDATEMANY );
				builder.setCriteria( cliQueryInfo.arguments.get( 0 ) );
				builder.setUpdateOrInsert( cliQueryInfo.arguments.get( 1 ) );
				if ( cliQueryInfo.arguments.size() > 2 ) {
					builder.setOptions( cliQueryInfo.arguments.get( 2 ) );
				}
				break;
			}
			case "replaceOne": {
				builder.setOperation( Operation.REPLACEONE );
				builder.setCriteria( cliQueryInfo.arguments.get( 0 ) );
				builder.setUpdateOrInsert( cliQueryInfo.arguments.get( 1 ) );
				if ( cliQueryInfo.arguments.size() > 2 ) {
					builder.setOptions( cliQueryInfo.arguments.get( 2 ) );
				}
				break;
			}
			case "aggregate": {
				builder.setOperation( Operation.AGGREGATE_PIPELINE );
				Document array = Document.parse( "{ 'dummy':" + cliQueryInfo.arguments.get( 0 ) + "}" );
				ArrayList keys = (ArrayList) array.get( "dummy" );
				keys.forEach( key -> {
					builder.getPipeline().add( (Document) key );
				} );
				break;
			}
			case "count": {
				builder.setOperation( Operation.COUNT );
				if ( cliQueryInfo.arguments.size() > 0 ) {
					builder.setCriteria( cliQueryInfo.arguments.get( 0 ) );
				}
				break;
			}
			case "distinct": {
				builder.setOperation( Operation.DISTINCT );
				builder.setDistinctFieldName( JSON.parse( cliQueryInfo.arguments.get( 0 ) ).toString() );
				if ( cliQueryInfo.arguments.size() > 1 ) {
					builder.setCriteria( cliQueryInfo.arguments.get( 1 ) );
				}
				if ( cliQueryInfo.arguments.size() > 2 ) {
					builder.setOptions( cliQueryInfo.arguments.get( 2 ) );
				}
				break;
			}
			case "mapReduce": {
				builder.setOperation( Operation.MAP_REDUCE );
				builder.setMapFunction( JSON.parse( cliQueryInfo.arguments.get( 0 ) ).toString() );
				builder.setReduceFunction( JSON.parse( cliQueryInfo.arguments.get( 1 ) ).toString() );
				if ( cliQueryInfo.arguments.size() > 2 ) {
					builder.setOptions( cliQueryInfo.arguments.get( 2 ) );
				}
				break;
			}
			default: {
				context.getParseErrors().add( createParseError( context, 0, "Unknown operation" + cliQueryInfo.operationName ) );
				return false;
			}
		}
		return true;
	};

	public Rule Query() {
		return Sequence(
				FirstOf( CriteriaOnlyFindQuery(), CliQuery() ),
				EOI,
				push( builder.build() )
				);
	}

	/**
	 * A find query only given as criterion.
	 *
	 * @return the {@link Rule} to identify a find query only
	 */
	public Rule CriteriaOnlyFindQuery() {
		return Sequence( JsonObject(), builder.setOperation( Operation.FIND ), builder.setCriteria( match() ) );
	}

	/**
	 * MongoDB CLI query.
	 *
	 * @return the {@link Rule} to identify a find query only
	 */
	public Rule CliQuery() {
		return Sequence(
				ZeroOrMore( WhiteSpace() ),
				PathExpression(), processPathExpression,
				ZeroOrMore( WhiteSpace() ),
				"( ", Arguments(), ") ", processCliQueryInfo
				);
	}

	public Rule PathExpression() {
		return Sequence( Identifier(), ZeroOrMore( Separator(), Identifier() ) );
	}

	@SuppressSubnodes
	public Rule Identifier() {
		return Sequence(
				OneOrMore( FirstOf( Letter(), Digit() ) ),
				cliQueryInfo.pathExpression.add( match() )
				);
	}

	public Rule Arguments() {
		return Optional( Sequence( Argument(), ZeroOrMore( ", ", Argument() ) ) );
	}

	public Rule Argument() {
		return Sequence(
				Value(),
				cliQueryInfo.arguments.add( match() )
				);
	}

	@SuppressNode
	public Rule Separator() {
		return Sequence( ZeroOrMore( WhiteSpace() ), ". " );
	}



	public Rule JsonComposite() {
		return FirstOf( JsonObject(), JsonArray() );
	}

	@SuppressNode
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
		return FirstOf( PrimitiveValue(), JsonComposite(), BsonFunctionCall() );
	}

	public Rule PrimitiveValue() {
		return FirstOf( JsonString(), JsonNumber(), "true ", "false ", "null ",
				"Infinity ", "NaN ", "undefined "
				);
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
	}

	@SuppressSubnodes
	public Rule JsonDoubleQuotedString() {
		return Sequence( "\"", ZeroOrMore( Character() ), "\" " );
	}

	@SuppressSubnodes
	public Rule JsonSingleQuotedString() {
		return Sequence( "'", ZeroOrMore( SingleQuotedStringCharacter() ), "' " );
	}

	@SuppressSubnodes
	public Rule BsonFunctionCall() {
		return Sequence( Optional( "new " ), SupportedBsonFunction(), ZeroOrMore( WhiteSpace() ), "( ",
				FirstOf(
						Sequence( PrimitiveValue(), ZeroOrMore( Sequence( ", ", PrimitiveValue() ) ) ),
						Optional( PrimitiveValue() )
						)
				, ") "
				);
	}

	public Rule SupportedBsonFunction() {
		return FirstOf(
				"BinData",
				"Date",
				"HexData",
				"ISODate",
				"NumberInt",
				"NumberLong",
				"ObjectId",
				"Timestamp",
				"RegExp",
				"DBPointer",
				"UUID",
				"GUID",
				"CSUUID",
				"CSGUID",
				"JUUID",
				"JGUID",
				"PYUUID",
				"PYGUID"
				);
	}

	public Rule Character() {
		return FirstOf( EscapedChar(), NormalChar() );
	}

	public Rule SingleQuotedStringCharacter() {
		return FirstOf( SingleQuotedStringEscapedChar(), SingleQuotedStringNormalChar() );
	}

	public Rule Letter() {
		return FirstOf( CharRange( 'a', 'z' ), CharRange( 'A', 'Z' ) );
	}

	public Rule EscapedChar() {
		return Sequence( "\\", FirstOf( AnyOf( "\"\\/bfnrt" ), Unicode() ) );
	}

	public Rule SingleQuotedStringEscapedChar() {
		return Sequence( "\\", FirstOf( AnyOf( "'\\/bfnrt" ), Unicode() ) );
	}

	public Rule NormalChar() {
		return Sequence( TestNot( AnyOf( "\"\\" ) ), ANY );
	}

	public Rule SingleQuotedStringNormalChar() {
		return Sequence( TestNot( AnyOf( "'\\" ) ), ANY );
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
		return FirstOf( CharRange( '0', '9' ), CharRange( 'a', 'f' ), CharRange( 'A', 'F' ) );
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
	protected Rule fromStringLiteral(final String string) {
		if ( string.endsWith( " " ) ) {
			return Sequence( string.trim(), Optional( WhiteSpace() ) );
		}
		else {
			return String( string );
		}
	}

	private ParseError createParseError(Context context, int errorLength, String errorMessage) {
		return new ParseError() {
			public InputBuffer getInputBuffer() {
				return context.getInputBuffer();
			}

			public int getStartIndex() {
				return context.getStartIndex();
			}

			public int getEndIndex() {
				return getStartIndex() + errorLength;
			}

			public String getErrorMessage() {
				return errorMessage;
			}
		};
	}




}
