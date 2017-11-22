/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.nativequery.impl;

import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor.Operation;

import com.mongodb.util.JSON;
import org.bson.json.JsonReader;
import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.annotations.SuppressSubnodes;

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
 * @author Dmitrii Masherov
 */
@BuildParseTree
public class NativeQueryParser extends BaseParser<MongoDBQueryDescriptorBuilder> {
	public Rule Query() {
		return Sequence( push( new MongoDBQueryDescriptorBuilder() ),  Optional( CliQueryOrJsonFindQuery() ) );
	}

	public Rule CliQueryOrJsonFindQuery() {
		return Sequence( FirstOf( ParsedQuery(), CriteriaOnlyFindQuery() ), peek().setQueryValid( true ), EOI );
	}

	public Rule ParsedQuery() {
		return Sequence( Db(),  Collection(),  Operation() );
	}

	/**
	 * A find query only given as criterion. Leave it to MongoDB's own parser to handle it.
	 *
	 * @return the {@link Rule} to identify a find query only
	 */
	public Rule CriteriaOnlyFindQuery() {
		return Sequence( !peek().isCliQuery(), JsonParameter( JsonObject() ) , peek().setOperation( Operation.FIND ), peek().setCriteria( match() ) );
	}


	@SuppressNode
	public Rule Db() {
		return Sequence( ZeroOrMore( WhiteSpace() ), "db ", peek().setCliQuery( true ), Separator() );
	}

	@SuppressSubnodes
	public Rule Collection() {
		return Sequence( PathExpression(), peek().setCollection( match() ), Separator() );
		//TODO OGM-949 it should not be just ANY matcher as they are some restrictions in the Collection naming in Mongo
		// cf. https://docs.mongodb.org/manual/faq/developers/#are-there-any-restrictions-on-the-names-of-collections
	}

	public Rule PathExpression() {
		return Sequence( Ident(), ZeroOrMore( Separator(), Ident(), TestNot( "( " ) ) );
	}

	@SuppressSubnodes
	public Rule Ident() {
		return Sequence(
				IdentCharacter(),
				ZeroOrMore( FirstOf( IdentCharacter(), Digit() ) ),
				ZeroOrMore( WhiteSpace() ) );
	}

	@SuppressNode
	public Rule Separator() {
		return Sequence( ZeroOrMore( WhiteSpace() ), ". " );
	}

	public Rule Reserved() {
		return FirstOf( Find(), FindOne(), FindAndModify(), Insert(), InsertOne(), InsertMany(), Remove(), DeleteOne(), DeleteMany(), Update(), UpdateOne(), UpdateMany(), ReplaceOne(), Count(), Aggregate(), Distinct(), MapReduce() );
		// TODO There are many more query types than what we support.
	}

	public Rule Operation() {
		return FirstOf(
				Find(),
				FindOne(),
				FindAndModify(),
				Insert(),
				InsertOne(),
				InsertMany(),
				Remove(),
				DeleteOne(),
				DeleteMany(),
				Update(),
				UpdateOne(),
				UpdateMany(),
				Count(),
				ReplaceOne(),
				Aggregate(),
				Distinct(),
				MapReduce(),
				Sequence( Optional( Ident(), peek().setOperationName( match() ) ), ACTION( false ) )
		);
	}

	public Rule Find() {
		return Sequence(
				"find ",
				peek().setOperation( Operation.FIND ),
				"( ",
				JsonParameter( JsonObject() ), peek().setCriteria( match() ),
				Optional( Sequence( ", ", JsonParameter( JsonObject() ), peek().setProjection( match() ) ) ),
				peek().setParametersValid( true ),
				") "
		);
	}

	public Rule FindOne() {
		return Sequence(
				"findOne ",
				peek().setOperation( Operation.FINDONE ),
				"( ",
				Optional( JsonParameter( JsonObject() ), peek().setCriteria( match() ) ),
				Optional( Sequence( ", ", JsonParameter( JsonObject() ), peek().setProjection( match() ) ) ),
				peek().setParametersValid( true ),
				") "
		);
	}

	public Rule FindAndModify() {
		return Sequence(
				"findAndModify ",
				peek().setOperation( Operation.FINDANDMODIFY ),
				"( ",
				JsonParameter( JsonObject() ), peek().setCriteria( match() ),
				peek().setParametersValid( true ),
				") "
		);
	}

	public Rule Insert() {
		return Sequence(
				"insert ",
				peek().setOperation( Operation.INSERT ),
				"( ",
				JsonParameter( JsonComposite() ), peek().setUpdateOrInsert( match() ),
				Optional( Sequence( ", ", JsonParameter( JsonObject() ), peek().setOptions( match() ) ) ),
				peek().setParametersValid( true ),
				") "
		);
	}

	public Rule InsertOne() {
		return Sequence(
				"insertOne ",
				peek().setOperation( Operation.INSERTONE ),
				"( ",
				JsonParameter( JsonComposite() ), peek().setUpdateOrInsert( match() ),
				Optional( Sequence( ", ", JsonParameter( JsonObject() ), peek().setOptions( match() ) ) ),
				peek().setParametersValid( true ),
				") "
		);
	}

	public Rule InsertMany() {
		return Sequence(
				"insertMany ",
				peek().setOperation( Operation.INSERTMANY ),
				"( ",
				JsonParameter( JsonComposite() ), peek().setUpdateOrInsert( match() ),
				Optional( Sequence( ", ", JsonParameter( JsonObject() ), peek().setOptions( match() ) ) ),
				peek().setParametersValid( true ),
				") "
		);
	}

	public Rule Remove() {
		return Sequence(
				"remove ",
				peek().setOperation( Operation.REMOVE ),
				"( ",
				JsonParameter( JsonObject() ), peek().setCriteria( match() ),
				Optional( Sequence( ", ",
					FirstOf(
						Sequence( BooleanValue(), peek().setOptions( "{ 'justOne': " + match() + " }" ) ),
						Sequence( JsonParameter( JsonObject() ), peek().setOptions( match() ) )
					)
				) ),
				peek().setParametersValid( true ),
				") "
		);
	}
	public Rule DeleteOne() {
		return Sequence(
				"deleteOne ",
				peek().setOperation( Operation.DELETEONE ),
				"( ",
				JsonParameter( JsonObject() ), peek().setCriteria( match() ),
				Optional( Sequence( ", ", JsonParameter( JsonObject() ), peek().setOptions( match() ) ) ),
				peek().setParametersValid( true ),
				") "
		);
	}
	public Rule DeleteMany() {
		return Sequence(
				"deleteMany ",
				peek().setOperation( Operation.DELETEMANY ),
				"( ",
				JsonParameter( JsonObject() ), peek().setCriteria( match() ),
				Optional( Sequence( ", ", JsonParameter( JsonObject() ), peek().setOptions( match() ) ) ),
				peek().setParametersValid( true ),
				") "
		);
	}

	public Rule Update() {
		return Sequence(
				"update ",
				peek().setOperation( Operation.UPDATE ),
				"( ",
				JsonParameter( JsonObject() ), peek().setCriteria( match() ), ", ",
				JsonParameter( JsonObject() ), peek().setUpdateOrInsert( match() ),
				Optional( Sequence( ", ", JsonParameter( JsonObject() ), peek().setOptions( match() ) ) ),
				peek().setParametersValid( true ),
				") "
		);
	}
	public Rule UpdateOne() {
		return Sequence(
				"updateOne ",
				peek().setOperation( Operation.UPDATEONE ),
				"( ",
				JsonParameter( JsonObject() ), peek().setCriteria( match() ), ", ",
				JsonParameter( JsonObject() ), peek().setUpdateOrInsert( match() ),
				Optional( Sequence( ", ", JsonParameter( JsonObject() ), peek().setOptions( match() ) ) ),
				peek().setParametersValid( true ),
				") "
		);
	}

	public Rule UpdateMany() {
		return Sequence(
				"updateMany ",
				peek().setOperation( Operation.UPDATEMANY ),
				"( ",
				JsonParameter( JsonObject() ), peek().setCriteria( match() ), ", ",
				JsonParameter( JsonObject() ), peek().setUpdateOrInsert( match() ),
				Optional( Sequence( ", ", JsonParameter( JsonObject() ), peek().setOptions( match() ) ) ),
				peek().setParametersValid( true ),
				") "
		);
	}

	public Rule ReplaceOne() {
		return Sequence(
				"replaceOne ",
				peek().setOperation( Operation.REPLACEONE ),
				"( ",
				JsonParameter( JsonObject() ), peek().setCriteria( match() ), ", ",
				JsonParameter( JsonObject() ), peek().setUpdateOrInsert( match() ),
				Optional( Sequence( ", ", JsonParameter( JsonObject() ), peek().setOptions( match() ) ) ),
				peek().setParametersValid( true ),
				") "
		);
	}

	public Rule Aggregate() {
		return Sequence(
				"aggregate ",
				peek().setOperation( Operation.AGGREGATE_PIPELINE ),
				"( ", AggregateArray(),
				peek().setParametersValid( true ),
				") "
		);
	}

	public Rule AggregateArray() {
		return Sequence(
				"[ ",
					Sequence(
						AggregateObject(),
						ZeroOrMore( Sequence( ", ", AggregateObject() ) ) ),
				"] " );
	}

	public Rule AggregateObject() {
		return Sequence(
				ZeroOrMore( WhiteSpace() ).skipNode(),
				"{ ", AggregatePair(), "} " );
	}

	public Rule AggregatePair() {
		return Sequence(
				JsonString(), peek().push( currentIndex(), match() ),
				": ",
				Value(), peek().addPipeline( peek().pop(), match() ) );
	}

	public Rule Count() {
		return Sequence(
				"count ",
				peek().setOperation( Operation.COUNT ),
				"( ",
				Optional( Sequence( JsonParameter( JsonComposite() ), peek().setCriteria( match() ) ) ),
				peek().setParametersValid( true ),
				") "
		);
	}

	public Rule Distinct() {
		return Sequence(
				"distinct ",
				peek().setOperation( Operation.DISTINCT ),
				"( ",
				Sequence( JsonString(), peek().setDistinctFieldName( readStringFromJson( match() ) ) ),
				Optional( Sequence( ", ", JsonParameter( JsonObject() ), peek().setCriteria( match() ) ) ),
				Optional( Sequence( ", ", JsonParameter( JsonObject() ), peek().setOptions( match() ) ) ),
				peek().setParametersValid( true ),
				") "
		);
	}

	public Rule MapReduce() {
		return Sequence(
				"mapReduce ",
				peek().setOperation( Operation.MAP_REDUCE ),
				"( ",
				Sequence( JsonString(), peek().setMapFunction( readStringFromJson( match() ) ) ),
				Sequence( ", ", JsonString(), peek().setReduceFunction( readStringFromJson( match() ) ) ),
				Optional( Sequence( ", ", JsonParameter( JsonObject() ), peek().setOptions( match() ) ) ),
				peek().setParametersValid( true ),
				") "
		);
	}

	public Rule JsonParameter(Rule Parameter) {
		return FirstOf(
				Parameter,
				Sequence( ZeroOrMore( ANY ), peek().setInvalidJsonParameter( match() ), ACTION( false )  )
		);
	}

	public Rule JsonComposite() {
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
		return Sequence( FirstOf( JsonString(), Ident() ), ": ", Value() );
	}

	public Rule Value() {
		return FirstOf( PrimitiveValue(), JsonComposite(), BsonFunctionCall() );
	}

	public Rule PrimitiveValue() {
		return FirstOf( JsonString(), JsonNumber(), "true ", "false ", "null ",
				"Infinity ", "NaN ", "undefined " );
	}

	public Rule BooleanValue() {
		return FirstOf( "true", "false" );
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
				, ") " );
	}

	public Rule SupportedBsonFunction() {
		return FirstOf( "BinData", "Date", "HexData", "ISODate", "NumberInt", "NumberLong", "ObjectId", "Timestamp", "RegExp", "DBPointer",
				"UUID", "GUID", "CSUUID", "CSGUID", "JUUID", "JGUID", "PYUUID", "PYGUID" );
	}

	public Rule IdentCharacter() {
		return FirstOf( '$', '_', CharRange( 'a', 'z' ), CharRange( 'A', 'Z' ) );
	}

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

	String readStringFromJson(String json) {
		try ( JsonReader jsonReader = new JsonReader( json ) ) {
			return jsonReader.readString();
		}
	}
}
