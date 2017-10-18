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
import org.hibernate.ogm.datastore.mongodb.utils.DocumentUtil;
import org.hibernate.ogm.utils.TestForIssue;

import org.junit.Test;

import org.bson.Document;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;


/**
 * Unit test for {@link NativeQueryParser}.
 *
 * @author Gunnar Morling
 * @see <a href="https://docs.mongodb.com/manual/tutorial/insert-documents/"> InsertOne documents in 3.4 API</a>
 */
public class NativeQueryParserTest {

	@Test
	@TestForIssue(jiraKey = "OGM-1024")
	public void shouldParseSimplifiedAggregateQuery() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		String match = "{ '$match': {'author' : 'Oscar Wilde' } }";
		String sort = "{ '$sort': {'name' : -1 } }";
		ParsingResult<MongoDBQueryDescriptorBuilder> run = new ReportingParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.aggregate([" + match + ", " + sort + " ])" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.AGGREGATE_PIPELINE );
		assertThat( queryDescriptor.getPipeline() )
			.containsExactly(
					Document.parse( match ),
					Document.parse( sort )
					);
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1024")
	public void shouldParseComplexAggregateQuery() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		String match = "{ '$match': { 'NAME':{'$regex':'Bangalore', '$options': 'i'}}}";
		String unwind = "{'$unwind': '$clicks'}";
		String group = "{ '$group': {'_id' : '$_id' ,'clicks' : {'$push':'$clicks'} ,'token' : { '$push': '$TOKEN' } } }";
		String sort = "{ '$sort': { '_id' : -1 } }";
		String nativeQuery = "db.UserFactualContent.aggregate(["
						+ match
						+ "," + unwind
						+ "," + group
						+ "," + sort
						+ "])";
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new ReportingParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() ).run( nativeQuery );

		System.out.println( ParseTreeUtils.printNodeTree( run ) );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "UserFactualContent" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.AGGREGATE_PIPELINE );
		assertThat( queryDescriptor.getPipeline() )
			.containsExactly(
					Document.parse( match )
					, Document.parse( unwind )
					, Document.parse( group )
					, Document.parse( sort )
					);
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1247")
	public void shouldParseDistinctQueryWithFieldOnly() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run = new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.distinct('item')" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.DISTINCT );
		assertThat( queryDescriptor.getCriteria() ).isNull();
		assertThat( queryDescriptor.getOptions() ).isNull();
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
		assertThat( queryDescriptor.getDistinctFieldName() ).isEqualTo( "item" );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1247")
	public void shouldParseDistinctQueryWithCriteriaAndCollation() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run = new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.distinct('item',{'orderId': { '$in': ['XYZ123', '123']}},{'collation' : {'locale' : 'fr'}})" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.DISTINCT );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ 'orderId' : { '$in': ['XYZ123', '123'] } }" ) );
		assertThat( queryDescriptor.getOptions() ).isEqualTo( Document.parse( "{'collation' : {'locale' : 'fr'}}" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
		assertThat( queryDescriptor.getDistinctFieldName() ).isEqualTo( "item" );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1247")
	public void shouldParseDistinctQueryWithCollationOnly() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.distinct('item', {}, {'collation' : {'locale' : 'fr'}})" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.DISTINCT );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{}" ) );
		assertThat( queryDescriptor.getOptions() ).isEqualTo( Document.parse( "{'collation' : {'locale' : 'fr'} }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
		assertThat( queryDescriptor.getDistinctFieldName() ).isEqualTo( "item" );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1246")
	public void shouldParseSimpleMapReduceQuery() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		String mapFunction = "function() { emit(this.cust_id, this.price); }";
		String reduceFuntion = "function(keyCustId, valuesPrices) { return Array.sum(valuesPrices); }";
		String query = "db.Order.mapReduce('" + mapFunction + "','" + reduceFuntion + "')";
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( query );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.MAP_REDUCE );
		assertThat( queryDescriptor.getMapFunction() ).isEqualTo( mapFunction );
		assertThat( queryDescriptor.getReduceFunction() ).isEqualTo( reduceFuntion );
		assertThat( queryDescriptor.getOptions() ).isNull();
		assertThat( queryDescriptor.getCriteria() ).isNull();
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
		assertThat( queryDescriptor.getDistinctFieldName() ).isNull();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1246")
	public void shouldParseComplexMapReduceQuery() {
		//https://docs.mongodb.com/manual/tutorial/map-reduce-examples/
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		String mapFunction = "function() { for (var idx = 0; idx < this.items.length; idx++) { var key = this.items[idx].sku; var value = { count: 1, qty: this.items[idx].qty }; emit(key, value); } }";
		String reduceFuntion = "function(keySKU, countObjVals) { reducedVal = { count: 0, qty: 0 }; for (var idx = 0; idx < countObjVals.length; idx++) { reducedVal.count += countObjVals[idx].count; reducedVal.qty += countObjVals[idx].qty; } return reducedVal; }";
		String finalizeFunction = "function (key, reducedVal) { reducedVal.avg = reducedVal.qty/reducedVal.count; return reducedVal; }";
		String options = "{ 'out': 'mycollection'," +
				"			'query': {}," +
				"			'sort' : {}," +
				"			'limit' : 1000," +
				"			'finalize' : '" + finalizeFunction + "'," +
				"			'scope' : {}," +
				"			'jsMode' : true," +
				"			'verbose' : false," +
				"			'bypassDocumentValidation' : false," +
				"			'collation' : {'locale' : 'fr'} }";

		String query = "db.Order.mapReduce('" + mapFunction + "','" + reduceFuntion + "'," + options + ")";
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( query );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.MAP_REDUCE );
		assertThat( queryDescriptor.getMapFunction() ).isEqualTo( mapFunction );
		assertThat( queryDescriptor.getReduceFunction() ).isEqualTo( reduceFuntion );
		assertThat( queryDescriptor.getOptions() ).isEqualTo( Document.parse( options ) );
		assertThat( queryDescriptor.getCriteria() ).isNull();
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
		assertThat( queryDescriptor.getDistinctFieldName() ).isNull();
	}

	@Test
	public void shouldParseSimplifiedFindQuery() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "{ \"foo\" : true }" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isNull();
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ \"foo\" : true }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseSimpleQuery() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.find({\"foo\":true})" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ \"foo\" : true }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseSimpleQueryUsingSingleQuotes() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.find( { 'foo' : true } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ \"foo\" : true }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseQueryWithEmptyFind() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.find({})" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( new Document() );
	}

	@Test
	public void shouldParseQueryInsertSingleDocument() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.insertOne( { 'item': 'card', 'qty': 15 } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.INSERTONE );
		assertThat( queryDescriptor.getUpdateOrInsertOne() ).isEqualTo( Document.parse( "{ 'item': 'card', 'qty': 15 }" ) );
	}

	@Test
	public void shouldParseQueryInsertSingleDocumentAndOptions() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.insertOne( { 'item': 'card', 'qty': 15 }, { 'ordered': true } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.INSERTONE );
		assertThat( queryDescriptor.getUpdateOrInsertOne() ).isEqualTo( Document.parse( "{ 'item': 'card', 'qty': 15 }" ) );
		assertThat( queryDescriptor.getUpdateOrInsertMany() ).isNull();
		assertThat( queryDescriptor.getOptions() ).isEqualTo( Document.parse( "{ ordered: true })" ) );
	}

	@Test
	public void shouldParseQueryInsertMultipleDocuments() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.insertMany( [ { '_id': 11, 'item': 'pencil', 'qty': 50, 'type': 'no.2' }, { 'item': 'pen', 'qty': 20 }, { 'item': 'eraser', 'qty': 25 } ] )" );
		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.INSERTMANY );
		assertThat( queryDescriptor.getUpdateOrInsertOne() ).isNull();
		assertThat( queryDescriptor.getUpdateOrInsertMany() ).isEqualTo( DocumentUtil.fromJsonArray( "[ { '_id': 11, 'item': 'pencil', 'qty': 50, 'type': 'no.2' }, { 'item': 'pen', 'qty': 20 }, { 'item': 'eraser', 'qty': 25 } ]" ) );
	}

	@Test
	public void shouldParseQueryInsertUnkwounDocuments() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.insert( [ { '_id': 11, 'item': 'pencil', 'qty': 50, 'type': 'no.2' }, { 'item': 'pen', 'qty': 20 }, { 'item': 'eraser', 'qty': 25 } ] )" );
		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.INSERT );
		assertThat( queryDescriptor.getUpdateOrInsertOne() ).isNull();
		assertThat( queryDescriptor.getUpdateOrInsertMany() ).isEqualTo( DocumentUtil.fromJsonArray(
					"[ { '_id': 11, 'item': 'pencil', 'qty': 50, 'type': 'no.2' }, { 'item': 'pen', 'qty': 20 }, { 'item': 'eraser', 'qty': 25 } ]" ) );
	}

	@Test
	public void shouldParseQueryInsertMultipleDocumentsAndOptions() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.insertMany( [ { '_id': 11, 'item': 'pencil', 'qty': 50, 'type': 'no.2' }, { 'item': 'pen', 'qty': 20 }, { 'item': 'eraser', 'qty': 25 } ], { 'ordered': true } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.INSERTMANY );
		assertThat( queryDescriptor.getUpdateOrInsertMany() ).isEqualTo( DocumentUtil.fromJsonArray(
				"[ { '_id': 11, 'item': 'pencil', 'qty': 50, 'type': 'no.2' }, { 'item': 'pen', 'qty': 20 }, { 'item': 'eraser', 'qty': 25 } ]" ) );
		assertThat( queryDescriptor.getOptions() ).isEqualTo( Document.parse( "{ ordered: true })" ) );
	}

	@Test
	public void shouldParseQueryWithEmptyRemove() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.remove( 	{\n 	}\n 	)" ); // Include superfluous whitespace.

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.REMOVE );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( new Document() );
	}

	@Test
	public void shouldParseQueryWithEmptyRemoveAndOptionalJustOne() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.remove({},true)" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.REMOVE );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( new Document() );
		assertThat( queryDescriptor.getOptions() ).isEqualTo( Document.parse( "{ \"justOne\" : true }" ) );
	}

	@Test
	public void shouldParseQueryWithEmptyRemoveAndOptions() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.remove( { }, { 'justOne': true } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.REMOVE );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( new Document() );
		assertThat( queryDescriptor.getOptions() ).isEqualTo( Document.parse( "{ \"justOne\" : true }" ) );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1313")
	public void shouldParseQueryDeleteOne() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.deleteOne( { } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.DELETEONE );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( new Document() );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1313")
	public void shouldParseQueryDeleteOneWithFilter() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.deleteOne( { 'item': 'card', 'qty': 15 } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.DELETEONE );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ 'item': 'card', 'qty': 15 }" ) );
		assertThat( queryDescriptor.getOptions() ).isNull();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1313")
	public void shouldParseQueryDeleteOneWithFilterAndOptions() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.deleteOne( { 'item': 'card', 'qty': 15 }, { 'w': 'majority', 'wtimeout' : 100 } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.DELETEONE );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ 'item': 'card', 'qty': 15 }" ) );
		assertThat( queryDescriptor.getOptions() ).isEqualTo( Document.parse( "{ 'w': 'majority', 'wtimeout' : 100 }" ) );
	}

	@Test
	public void shouldParseQueryUpdate() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.update( { 'name': 'Andy' }, { 'rating': 1, 'score': 1 } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.UPDATE );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ 'name': 'Andy' }" ) );
		assertThat( queryDescriptor.getUpdateOrInsertOne() ).isEqualTo( Document.parse( "{ 'rating': 1, 'score': 1 }" ) );
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseQueryUpdateWithOptions() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.update( { 'name': 'Andy' }, { 'rating': 1, 'score': 1 }, { 'upsert': true } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.UPDATE );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ 'name': 'Andy' }" ) );
		assertThat( queryDescriptor.getUpdateOrInsertOne() ).isEqualTo( Document.parse( "{ 'rating': 1, 'score': 1 }" ) );
		assertThat( queryDescriptor.getOptions() ).isEqualTo( Document.parse( "{ 'upsert': true }" ) );
	}

	@Test
	public void shouldParseQueryUpdateOne() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.updateOne( { 'name' : 'Andy' }, { '$set': { 'score' : 3 } } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.UPDATEONE );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ 'name' : 'Andy' }" ) );
		assertThat( queryDescriptor.getUpdateOrInsertOne() ).isEqualTo( Document.parse( "{ '$set': { 'score' : 3 } }" ) );
		assertThat( queryDescriptor.getOptions() ).isNull();
	}

	@Test
	public void shouldParseQueryUpdateOneWithOptions() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.updateOne( { 'name' : 'Andy' }, { '$set': { 'score' : 3 } }, { 'upsert': true, 'collation': {} } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.UPDATEONE );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ 'name' : 'Andy' }" ) );
		assertThat( queryDescriptor.getUpdateOrInsertOne() ).isEqualTo( Document.parse( "{ '$set': { 'score' : 3 } }" ) );
		assertThat( queryDescriptor.getOptions() ).isEqualTo( Document.parse( "{ 'upsert': true, 'collation': {} }" ) );
	}

	@Test
	public void shouldParseQueryUpdateMany() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.updateMany( { 'name': 'Andy' }, { '$mul': { 'score': 5 } } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.UPDATEMANY );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ 'name': 'Andy' }" ) );
		assertThat( queryDescriptor.getUpdateOrInsertOne() ).isEqualTo( Document.parse( "{ '$mul': { 'score': 5 } }" ) );
		assertThat( queryDescriptor.getOptions() ).isNull();
	}

	@Test
	public void shouldParseQueryUpdateManyWithOptions() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.updateMany( { 'name' : 'Andy' }, { '$mul': { 'score': 5 } }, { 'upsert': true, 'writeConcern': {'w': 'majority', 'wtimeout' : 100 } } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.UPDATEMANY );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ 'name' : 'Andy' }" ) );
		assertThat( queryDescriptor.getUpdateOrInsertOne() ).isEqualTo( Document.parse( "{ '$mul': { 'score': 5 } }" ) );
		assertThat( queryDescriptor.getOptions() ).isEqualTo( Document.parse( "{ 'upsert': true, 'writeConcern': {'w': 'majority', 'wtimeout' : 100 } }" ) );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1317")
	public void shouldParseQueryReplaceOneEmptyCriteriaEmptyReplacementNoOptions() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run = new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>(
				parser.Query() ).run( "db.Order.replaceOne( { }, { } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.REPLACEONE );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ }" ) );
		assertThat( queryDescriptor.getUpdateOrInsertOne() ).isEqualTo( Document.parse( "{ }" ) );
		assertThat( queryDescriptor.getOptions() ).isNull();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1317")
	public void shouldParseQueryReplaceOneEmptyCriteriaWithReplacementNoOptions() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run = new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>(
				parser.Query() ).run( "db.Order.replaceOne( { }, { 'name' : 'Lulu', 'age' : 18, 'score' : 22 } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.REPLACEONE );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ }" ) );
		assertThat( queryDescriptor.getUpdateOrInsertOne() ).isEqualTo(
				Document.parse( "{ 'name' : 'Lulu', 'age' : 18, 'score' : 22 }" ) );
		assertThat( queryDescriptor.getOptions() ).isNull();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1317")
	public void shouldParseQueryReplaceOneWithCriteriaWithReplacementNoOptions() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run = new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>(
				parser.Query() ).run(
				"db.Order.replaceOne( { 'name' : 'Andy' }, { 'name' : 'Lulu', 'age' : 18, 'score' : 22 } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.REPLACEONE );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ 'name' : 'Andy' }" ) );
		assertThat( queryDescriptor.getUpdateOrInsertOne() ).isEqualTo(
				Document.parse( "{ 'name' : 'Lulu', 'age' : 18, 'score' : 22 }" ) );
		assertThat( queryDescriptor.getOptions() ).isNull();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1317")
	public void shouldParseQueryReplaceOneWithCriteriaWithReplacementWithOptions() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run = new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>(
				parser.Query() ).run(
				"db.Order.replaceOne( { 'name' : 'Andy' }, { 'name' : 'Lulu', 'age' : 18, 'score' : 22 }, " +
						"{ 'upsert': true, 'writeConcern': { 'w': 'majority', 'wtimeout' : 100 }, 'collation': { 'locale': 'fr_CA' } } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.REPLACEONE );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ 'name' : 'Andy' }" ) );
		assertThat( queryDescriptor.getUpdateOrInsertOne() ).isEqualTo(
				Document.parse( "{ 'name' : 'Lulu', 'age' : 18, 'score' : 22 }" ) );
		assertThat( queryDescriptor.getOptions() ).isEqualTo( Document.parse(
				"{ 'upsert': true, 'writeConcern': { 'w': 'majority', 'wtimeout' : 100 }, 'collation': { 'locale': 'fr_CA' } }" ) );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1317")
	public void shouldParseQueryReplaceOneWithWhitespaces() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run = new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>(
				parser.Query() ).run(
				"  db  .  Order  . \t\n replaceOne(   \t\n{   'name' :   'Andy'  \t\n },   " +
						"{   'name'   :   'Lulu',   'age'   :   18, 'score' :   22   }   )  " );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.REPLACEONE );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ 'name' : 'Andy' }" ) );
		assertThat( queryDescriptor.getUpdateOrInsertOne() ).isEqualTo(
				Document.parse( "{ 'name' : 'Lulu', 'age' : 18, 'score' : 22 }" ) );
		assertThat( queryDescriptor.getOptions() ).isNull();
	}

	@Test
	public void shouldParseQueryFindAndModify() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.findAndModify( { 'query': { 'name': 'Andy' }, 'sort': { 'rating': 1 }, 'update': { '$inc': { 'score': 1 } }, 'upsert': true } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FINDANDMODIFY );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse(
				"{ 'query': { 'name': 'Andy' }, 'sort': { 'rating': 1 }, 'update': { '$inc': { 'score': 1 } }, 'upsert': true }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseQueryFindOneWithoutCriteriaNorProjection() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.findOne(  )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FINDONE );
		assertThat( queryDescriptor.getCriteria() ).isNull();
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseQueryFindOneWithoutProjection() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.findOne( { \"foo\" : true } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FINDONE );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ \"foo\" : true }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseQueryFindOneWithCriteriaAndProjection() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.findOne( { \"foo\" : true }, { \"foo\" : 1 } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FINDONE );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ \"foo\" : true }" ) );
		assertThat( queryDescriptor.getProjection() ).isEqualTo( Document.parse( "{ \"foo\" : 1 }" ) );
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseQueryWithProjection() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.find( { \"foo\" : true }, { \"foo\" : 1 } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ \"foo\" : true }" ) );
		assertThat( queryDescriptor.getProjection() ).isEqualTo( Document.parse( "{ \"foo\" : 1 }" ) );
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseQueryWithWhitespace() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "  db  .  Order  .  find  (  {  \"  foo  \"  :  true  }  ,  {  \"foo\"  :  1  }  )  " );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();
		assertThat( run.hasErrors() ).isFalse();
		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ \"  foo  \" : true }" ) );
		assertThat( queryDescriptor.getProjection() ).isEqualTo( Document.parse( "{ \"foo\" : 1 }" ) );
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseQueryWithSeveralConditions() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ReportingParseRunner<MongoDBQueryDescriptorBuilder> runner = new ReportingParseRunner<>( parser.Query() );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  runner
				.run( "db.Order.find( { \"foo\" : true, \"bar\" : 42, \"baz\" : \"qux\" } )" );

		assertThat( run.hasErrors() ).isFalse();
		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ \"foo\" : true, \"bar\" : 42, \"baz\" : \"qux\" }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseCountQuery() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.count()" );

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
				.run( "db.Order.count( { 'foo' : true } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.COUNT );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ 'foo' : true }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseCountQueryWithLogicalOperatorOR() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.count( { '$or': [ { 'foo' : true }, { 'bar' : '42' } ] } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.COUNT );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ '$or': [ { 'foo' : true }, { 'bar' : '42' } ] } }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseCountQueryWithLogicalOperatorAND() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.count( { '$and': [ { 'foo' : true }, { 'bar' : '42' } ] } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.COUNT );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ '$and': [ { 'foo' : true }, { 'bar' : '42' } ] } }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseCountQueryWithLogicalOperatorNOR() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.count( { '$nor': [ { 'foo' : true }, { 'bar' : '42' } ] } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.COUNT );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ '$nor': [ { 'foo' : true }, { 'bar' : '42' } ] } }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseCountQueryWithLogicalOperatorNOT() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.count( { '$not': { 'foo' : false } } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.COUNT );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ '$not': { 'foo' : false } } }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseFindQueryWithLogicalOperatorOR() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.find( { '$or': [ { 'foo' : true }, { 'bar' : '42' } ] } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ '$or': [ { 'foo' : true }, { 'bar' : '42' } ] } }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldParseFindQueryWithLogicalOperatorAND() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.find( { '$and': [ { 'foo' : true }, { 'bar' : '42' } ] } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ '$and': [ { 'foo' : true }, { 'bar' : '42' } ] } }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldFindCountQueryWithLogicalOperatorNOR() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.find( { '$nor': [ { 'foo' : true }, { 'bar' : '42' } ] } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ '$nor': [ { 'foo' : true }, { 'bar' : '42' } ] } }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	public void shouldFindeCountQueryWithLogicalOperatorNOT() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.Order.find( { '$not': { 'foo' : false } } )" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "Order" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.FIND );
		assertThat( queryDescriptor.getCriteria() ).isEqualTo( Document.parse( "{ '$not': { 'foo' : false } } }" ) );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-900")
	public void shouldSupportDotInCollectionName() {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> run =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( "db.POEM.COM.count()" );

		MongoDBQueryDescriptor queryDescriptor = run.resultValue.build();

		assertThat( queryDescriptor.getCollectionName() ).isEqualTo( "POEM.COM" );
		assertThat( queryDescriptor.getOperation() ).isEqualTo( Operation.COUNT );
		assertThat( queryDescriptor.getProjection() ).isNull();
		assertThat( queryDescriptor.getOrderBy() ).isNull();
	}


}
