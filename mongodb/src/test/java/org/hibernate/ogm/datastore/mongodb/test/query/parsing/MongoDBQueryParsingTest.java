/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query.parsing;

import static org.fest.assertions.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.hibernate.hql.QueryParser;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.mongodb.query.parsing.impl.MongoDBProcessingChain;
import org.hibernate.ogm.datastore.mongodb.query.parsing.impl.MongoDBQueryParsingResult;
import org.hibernate.ogm.datastore.mongodb.test.query.parsing.model.IndexedEntity;
import org.hibernate.ogm.datastore.mongodb.test.query.parsing.model.inheritance.singletable.CommunityMemberST;
import org.hibernate.ogm.datastore.mongodb.test.query.parsing.model.inheritance.singletable.EmployeeST;
import org.hibernate.ogm.datastore.mongodb.test.query.parsing.model.inheritance.singletable.PersonST;
import org.hibernate.ogm.datastore.mongodb.utils.MapBasedEntityNamesResolver;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for {@link org.hibernate.ogm.datastore.mongodb.query.parsing.impl.MongoDBQueryRendererDelegate}.
 *
 * @author Gunnar Morling
 */
public class MongoDBQueryParsingTest extends OgmTestCase {

	private Log log = LoggerFactory.make( MethodHandles.lookup() );

	@SuppressWarnings("deprecation")
	private JsonWriterSettings jsonWriterSettings = JsonWriterSettings.builder().outputMode( JsonMode.STRICT ).build();

	private QueryParser queryParser;

	@Before
	public void setupParser() {
		queryParser = new QueryParser();
	}

	@Test
	public void shouldCreateUnrestrictedQuery() {
		assertMongoDbQuery(
				"from IndexedEntity",
				"{}" );
	}

	@Test
	public void shouldCreateQueryWithSingleDiscriminatorValue() {
		assertMongoDbQuery(
				"from EmployeeST",
				"{\"DTYPE\": \"EMP\"}",
				EmployeeST.class );
	}

	@Test
	public void shouldCreateQueryWithSingleDiscriminatorValueWithFilter() {
		assertMongoDbQuery(
				"from EmployeeST e where e.employer = 'Red Hat'",
				"{\"$and\": [" +
					"{\"employer\": \"Red Hat\"}, " +
					"{\"DTYPE\": \"EMP\"}" +
				"]}",
				EmployeeST.class );
	}

	@Test
	public void shouldCreateQueryWithMultipleDiscriminatorValues() {
		assertMongoDbQuery(
				"from CommunityMemberST",
				"{\"DTYPE\": " +
					"{\"$in\": [\"CMM\", \"EMP\"]}" +
				"}",
				CommunityMemberST.class );
	}

	@Test
	public void shouldCreateQueryWithMultipleDiscriminatorValuesWithFilter() {
		assertMongoDbQuery(
				"from CommunityMemberST c where c.project = 'Hibernate OGM'",
				"{\"$and\": [" +
					"{\"project\": \"Hibernate OGM\"}, " +
					"{\"DTYPE\": " +
						"{\"$in\": [\"CMM\", \"EMP\"]}" +
					"}" +
				"]}",
				CommunityMemberST.class );
	}

	@Test
	public void shouldCreateRestrictedQueryUsingSelect() {
		assertMongoDbQuery(
				"select e from IndexedEntity e where e.title = 'same'",
				"{\"title\": \"same\"}" );
	}

	@Test
	public void shouldUseSpecialNameForIdPropertyInWhereClause() {
		assertMongoDbQuery(
				"select e from IndexedEntity e where e.id = '1'",
				"{\"_id\": \"1\"}" );
	}

	@Test
	public void shouldUseColumnNameForPropertyInWhereClause() {
		assertMongoDbQuery(
				"select e from IndexedEntity e where e.name = 'Bob'",
				"{\"entityName\": \"Bob\"}" );
	}

	@Test
	public void shouldCreateProjectionQuery() {
		MongoDBQueryParsingResult parsingResult = parseQuery( "select e.id, e.name, e.position from IndexedEntity e" );

		assertThat( parsingResult.getQuery().toJson( jsonWriterSettings ) ).isEqualTo( "{}" );
		assertThat( parsingResult.getProjection().toJson( jsonWriterSettings ) )
				.isEqualTo( "{\"_id\": 1, \"entityName\": 1, \"position\": 1}" );
	}

	@Test
	public void shouldAddNumberPropertyAsNumber() {
		assertMongoDbQuery(
				"select e from IndexedEntity e where e.position = 2",
				"{\"position\": {\"$numberLong\": \"2\"}}" );
	}

	@Test
	public void shouldCreateLessOrEqualQuery() {
		assertMongoDbQuery(
				"select e from IndexedEntity e where e.position <= 20",
				"{\"position\": {\"$lte\": {\"$numberLong\": \"20\"}}}" );
	}

	@Test
	public void shouldCreateQueryWithNegationInWhereClause() {
		assertMongoDbQuery(
				"select e from IndexedEntity e where e.name <> 'Bob'",
				"{\"entityName\": {\"$ne\": \"Bob\"}}" );
	}

	@Test
	public void shouldCreateQueryWithNestedNegationInWhereClause() {
		assertMongoDbQuery(
				"select e from IndexedEntity e where NOT e.name <> 'Bob'",
				"{\"entityName\": \"Bob\"}" );
	}

	@Test
	public void shouldCreateQueryUsingSelectWithConjunctionInWhereClause() {
		assertMongoDbQuery(
				"select e from IndexedEntity e where e.title = 'same' and e.position = 1",
				"{\"$and\": [" +
					"{\"title\": \"same\"}, " +
					"{\"position\": {\"$numberLong\": \"1\"}}" +
				"]}" );
	}

	@Test
	public void shouldCreateQueryWithNegationAndConjunctionInWhereClause() {
		assertMongoDbQuery(
				"select e from IndexedEntity e where NOT ( e.name = 'Bob' AND e.position = 1 )",
				"{\"$or\": [" +
					"{\"entityName\": {\"$ne\": \"Bob\"}}, " +
					"{\"position\": {\"$ne\": {\"$numberLong\": \"1\"}}}" +
				"]}" );
	}

	@Test
	public void shouldCreateNegatedRangeQuery() {
		assertMongoDbQuery(
				"select e from IndexedEntity e where e.name = 'Bob' and not e.position between 1 and 3",
				"{\"$and\": [" +
					"{\"entityName\": \"Bob\"}, " +
					"{\"$or\": [" +
						"{\"position\": {\"$lt\": {\"$numberLong\": \"1\"}}}, " +
						"{\"position\": {\"$gt\": {\"$numberLong\": \"3\"}}}" +
					"]}" +
				"]}" );
	}

	@Test
	public void shouldCreateBooleanQueryUsingSelect() {
		assertMongoDbQuery(
				"select e from IndexedEntity e where e.name = 'same' or ( e.id = 4 and e.name = 'booh')",
				"{\"$or\": [" +
					"{\"entityName\": \"same\"}, " +
					"{\"$and\": [" +
						"{\"_id\": \"4\"}, " +
						"{\"entityName\": \"booh\"}" +
					"]}" +
				"]}" );
	}

	@Test
	public void shouldCreateNumericBetweenQuery() {
		Map<String, Object> namedParameters = new HashMap<String, Object>();
		namedParameters.put( "lower", 10L );
		namedParameters.put( "upper", 20L );

		assertMongoDbQuery(
				"select e from IndexedEntity e where e.position between :lower and :upper",
				namedParameters,
				"{\"$and\": [" +
					"{\"position\": {\"$gte\": {\"$numberLong\": \"10\"}}}, " +
					"{\"position\": {\"$lte\": {\"$numberLong\": \"20\"}}}" +
				"]}" );
	}

	@Test
	public void shouldCreateInQuery() {
		assertMongoDbQuery(
				"select e from IndexedEntity e where e.title IN ( 'foo', 'bar', 'same')",
				"{\"title\": " +
					"{\"$in\": [\"foo\", \"bar\", \"same\"]}" +
				"}" );
	}

	@Test
	public void shouldCreateNotInQuery() {
		assertMongoDbQuery(
				"select e from IndexedEntity e where e.title NOT IN ( 'foo', 'bar', 'same')",
				"{\"title\": " +
					"{\"$nin\": [\"foo\", \"bar\", \"same\"]}" +
				"}" );
	}

	@Test
	public void shouldCreateLikeQuery() {
		assertMongoDbQuery(
				"select e from IndexedEntity e where e.title like 'Ali_e%'",
				"{\"title\": " +
					"{\"$regex\": \"^\\\\QAli\\\\E.\\\\Qe\\\\E.*$\", " +
					"\"$options\": \"s\"" +
					"}" +
				"}" );
	}

	@Test
	public void shouldCreateNotLikeQuery() {
		assertMongoDbQuery(
				"select e from IndexedEntity e where e.title not like 'Ali_e%'",
				"{\"title\": " +
					"{\"$not\": " +
						"{\"$regex\": \"^\\\\QAli\\\\E.\\\\Qe\\\\E.*$\", " +
						"\"$options\": \"s\"" +
						"}" +
					"}" +
				"}" );
	}

	@Test
	public void shouldCreateIsNullQuery() {
		assertMongoDbQuery(
				"select e from IndexedEntity e where e.title is null",
				"{\"title\": " +
					"{\"$exists\": false}" +
				"}" );
	}

	@Test
	public void shouldCreateIsNotNullQuery() {
		assertMongoDbQuery(
				"select e from IndexedEntity e where e.title is not null",
				"{\"title\": " +
					"{\"$exists\": true}" +
				"}" );
	}

	private void assertMongoDbQuery(String queryString, String expectedMongoDbQuery) {
		assertMongoDbQuery( queryString, null, expectedMongoDbQuery );
	}

	private void assertMongoDbQuery(String queryString, String expectedMongoDbQuery, Class<?> expectedEntityType) {
		assertMongoDbQuery( queryString, null, expectedMongoDbQuery, expectedEntityType );
	}

	private void assertMongoDbQuery(String queryString, Map<String, Object> namedParameters, String expectedMongoDbQuery) {
		assertMongoDbQuery( queryString, namedParameters, expectedMongoDbQuery, IndexedEntity.class );
	}

	private void assertMongoDbQuery(String queryString, Map<String, Object> namedParameters, String expectedMongoDbQuery, Class<?> expectedEntityType) {
		MongoDBQueryParsingResult parsingResult = parseQuery( queryString, namedParameters );
		assertThat( parsingResult ).isNotNull();
		assertThat( parsingResult.getEntityType() ).isSameAs( expectedEntityType );

		if ( expectedMongoDbQuery == null ) {
			assertThat( parsingResult.getQuery() ).isNull();
		}
		else {
			assertThat( parsingResult.getQuery() ).isNotNull();
			log.debugf( "expectedMongoDbQuery: %s", expectedMongoDbQuery );
			log.debugf( "  actualMongoDbQuery: %s", parsingResult.getQuery().toJson( jsonWriterSettings ) );

			assertThat( parsingResult.getQuery().toJson( jsonWriterSettings ) ).isEqualTo( expectedMongoDbQuery );
		}
	}

	private MongoDBQueryParsingResult parseQuery(String queryString) {
		return parseQuery( queryString, null );
	}

	private MongoDBQueryParsingResult parseQuery(String queryString, Map<String, Object> namedParameters) {
		return queryParser.parseQuery(
				queryString,
				setUpMongoDbProcessingChain( namedParameters )
				);
	}

	private MongoDBProcessingChain setUpMongoDbProcessingChain(Map<String, Object> namedParameters) {
		Map<String, Class<?>> entityNames = new HashMap<String, Class<?>>();
		entityNames.put( "com.acme.IndexedEntity", IndexedEntity.class );
		entityNames.put( "IndexedEntity", IndexedEntity.class );
		entityNames.put( "CommunityMemberST", CommunityMemberST.class );
		entityNames.put( "PersonST", PersonST.class );
		entityNames.put( "EmployeeST", EmployeeST.class );
		EntityNamesResolver nameResolver = new MapBasedEntityNamesResolver( entityNames );

		return new MongoDBProcessingChain( getSessionFactory(), nameResolver, namedParameters );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ IndexedEntity.class, PersonST.class, CommunityMemberST.class, EmployeeST.class };
	}
}
