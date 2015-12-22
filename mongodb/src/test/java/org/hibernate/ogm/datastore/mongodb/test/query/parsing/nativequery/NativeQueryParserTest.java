/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query.parsing.nativequery;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor;
import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor.Operation;
import org.hibernate.ogm.datastore.mongodb.query.parsing.nativequery.impl.MongoDBQueryDescriptorBuilder;
import org.hibernate.ogm.datastore.mongodb.query.parsing.nativequery.impl.NativeQueryParser;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.Test;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;

/**
 * Unit test for {@link NativeQueryParser}.
 *
 * @author Gunnar Morling
 */
public class NativeQueryParserTest {

	@Test
	public void shouldParseSimplifiedFindQuery() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "{ \"foo\" : true }");

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isNull();
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( JSON.parse( "{ \"foo\" : true }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseSimpleQuery() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.find({\"foo\":true})");

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( JSON.parse( "{ \"foo\" : true }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseSimpleQueryUsingSingleQuotes() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.find( { 'foo' : true } )");

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( JSON.parse( "{ \"foo\" : true }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseQueryWithEmptyFind() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.find({})");

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( new BasicDBObject() );
	}

	@Test
	public void shouldParseQueryWithProjection() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.find( { \"foo\" : true }, { \"foo\" : 1 } )");

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( JSON.parse( "{ \"foo\" : true }" ) );
		assertThat( queryDescriptor.getProjection() ).isEqualTo( JSON.parse( "{ \"foo\" : 1 }" ) );
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseQueryWithWhitespace() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "  db  .  Order  .  find  (  {  \"  foo  \"  :  true  }  ,  {  \"foo\"  :  1  }  )  ");

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( run.hasErrors() ).isFalse();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( JSON.parse( "{ \"  foo  \" : true }" ) );
		assertThat( queryDescriptor.getProjection() ).isEqualTo( JSON.parse( "{ \"foo\" : 1 }" ) );
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseQueryWithSeveralConditions() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ReportingParseRunner<MongoDBQueryDescriptorBuilder> runner = new ReportingParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  runner
				.run( "db.Order.find( { \"foo\" : true, \"bar\" : 42, \"baz\" : \"qux\" } )");

		assertThat( run.hasErrors() ).isFalse();
		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( JSON.parse( "{ \"foo\" : true, \"bar\" : 42, \"baz\" : \"qux\" }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseCountQuery() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.count()");

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.COUNT );
		assertThat( queryDescriptor.getCriteria() ).isNull();
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseCountQueryWithCriteria() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.count( { 'foo' : true } )");

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.COUNT );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( JSON.parse( "{ 'foo' : true }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseCountQueryWithLogicalOperatorOR() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.count( { '$or': [ { 'foo' : true }, { 'bar' : '42' } ] } )");

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.COUNT );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( JSON.parse( "{ '$or': [ { 'foo' : true }, { 'bar' : '42' } ] } }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseCountQueryWithLogicalOperatorAND() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.count( { '$and': [ { 'foo' : true }, { 'bar' : '42' } ] } )");

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.COUNT );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( JSON.parse( "{ '$and': [ { 'foo' : true }, { 'bar' : '42' } ] } }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseCountQueryWithLogicalOperatorNOR() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.count( { '$nor': [ { 'foo' : true }, { 'bar' : '42' } ] } )");

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.COUNT );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( JSON.parse( "{ '$nor': [ { 'foo' : true }, { 'bar' : '42' } ] } }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseCountQueryWithLogicalOperatorNOT() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.count( { '$not': { 'foo' : false } } )");

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.COUNT );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( JSON.parse( "{ '$not': { 'foo' : false } } }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseFindQueryWithLogicalOperatorOR() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.find( { '$or': [ { 'foo' : true }, { 'bar' : '42' } ] } )");

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( JSON.parse( "{ '$or': [ { 'foo' : true }, { 'bar' : '42' } ] } }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseFindQueryWithLogicalOperatorAND() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.find( { '$and': [ { 'foo' : true }, { 'bar' : '42' } ] } )");

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( JSON.parse( "{ '$and': [ { 'foo' : true }, { 'bar' : '42' } ] } }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldFindCountQueryWithLogicalOperatorNOR() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.find( { '$nor': [ { 'foo' : true }, { 'bar' : '42' } ] } )");

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( JSON.parse( "{ '$nor': [ { 'foo' : true }, { 'bar' : '42' } ] } }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldFindeCountQueryWithLogicalOperatorNOT() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.find( { '$not': { 'foo' : false } } )");

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( JSON.parse( "{ '$not': { 'foo' : false } } }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-900")
	public void shouldSupportDotInCollectionName() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.POEM.COM.count()");

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "POEM.COM" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.COUNT );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}


}
