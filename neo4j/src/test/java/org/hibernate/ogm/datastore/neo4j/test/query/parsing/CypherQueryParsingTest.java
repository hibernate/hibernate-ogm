/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.query.parsing;

import static org.fest.assertions.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.hql.QueryParser;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.Neo4jProcessingChain;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.Neo4jQueryParsingResult;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.Neo4jQueryRendererDelegate;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.Neo4jQueryResolverDelegate;
import org.hibernate.ogm.datastore.neo4j.test.query.parsing.model.IndexedEntity;
import org.hibernate.ogm.datastore.neo4j.test.query.parsing.model.inheritance.CommunityMemberST;
import org.hibernate.ogm.datastore.neo4j.test.query.parsing.model.inheritance.EmployeeST;
import org.hibernate.ogm.datastore.neo4j.test.query.parsing.model.inheritance.PersonST;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.parser.MapBasedEntityNamesResolver;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for {@link Neo4jQueryRendererDelegate} and {@link Neo4jQueryResolverDelegate}.
 *
 * @author Davide D'Alto
 */
public class CypherQueryParsingTest extends OgmTestCase {

	private QueryParser queryParser;

	@Before
	public void setupParser() {
		queryParser = new QueryParser();
	}

	@Test
	public void shouldCreateUnrestrictedQuery() {
		assertQuery(
				"from IndexedEntity",
				"MATCH (`<gen:0>`:IndexedEntity) RETURN `<gen:0>`" );
	}

	@Test
	public void shouldCreateQueryWithSingleDiscriminatorValue() {
		assertQuery(
				"from EmployeeST",
				"MATCH (`<gen:0>`:PersonST) WHERE `<gen:0>`.DTYPE = \"EMP\" RETURN `<gen:0>`",
				EmployeeST.class );
	}

	@Test
	public void shouldCreateQueryWithSingleDiscriminatorValueWithFilter() {
		assertQuery(
				"from EmployeeST e where e.employer = 'Red Hat'",
				"MATCH (e:PersonST) WHERE e.employer = \"Red Hat\" AND e.DTYPE = \"EMP\" RETURN e",
				EmployeeST.class );
	}

	@Test
	public void shouldCreateQueryWithMultipleDiscriminatorValues() {
		assertQuery(
				"from CommunityMemberST",
				"MATCH (`<gen:0>`:PersonST) WHERE `<gen:0>`.DTYPE IN [\"EMP\", \"CMM\"] RETURN `<gen:0>`",
				CommunityMemberST.class );
	}

	@Test
	public void shouldCreateQueryWithMultipleDiscriminatorValuesWithFilter() {
		assertQuery(
				"from CommunityMemberST c where c.project = 'Hibernate OGM'",
				"MATCH (c:PersonST) WHERE c.project = \"Hibernate OGM\" AND c.DTYPE IN [\"EMP\", \"CMM\"] RETURN c",
				CommunityMemberST.class );
	}

	@Test
	public void shouldCreateRestrictedQueryUsingSelect() {
		assertQuery(
				"select e from IndexedEntity e where e.title = 'same'",
				"MATCH (e:IndexedEntity) WHERE e.title = \"same\" RETURN e" );
	}

	@Test
	public void shouldUseSpecialNameForIdPropertyInWhereClause() {
		assertQuery(
				"select e from IndexedEntity e where e.id = '1'",
				"MATCH (e:IndexedEntity) WHERE e.id = \"1\" RETURN e" );
	}

	@Test
	public void shouldUseColumnNameForPropertyInWhereClause() {
		assertQuery(
				"select e from IndexedEntity e where e.name = 'Bob'",
				"MATCH (e:IndexedEntity) WHERE e.entityName = \"Bob\" RETURN e" );
	}

	@Test
	public void shouldCreateProjectionQuery() {
		Neo4jQueryParsingResult parsingResult = parseQuery( "select e.id, e.name, e.position from IndexedEntity e" );

		assertThat( parsingResult.getQueryObject() ).isEqualTo( "MATCH (e:IndexedEntity) RETURN e.id, e.entityName, e.position" );
		assertThat( parsingResult.getProjections() ).containsExactly( "e.id", "e.entityName", "e.position" );
	}

	@Test
	public void shouldAddNumberPropertyAsNumber() {
		assertQuery(
				"select e from IndexedEntity e where e.position = 2",
				"MATCH (e:IndexedEntity) WHERE e.position = 2 RETURN e" );
	}

	@Test
	public void shouldCreateLessOrEqualQuery() {
		assertQuery(
				"select e from IndexedEntity e where e.position <= 20",
				"MATCH (e:IndexedEntity) WHERE e.position <= 20 RETURN e" );
	}

	@Test
	public void shouldCreateQueryWithNegationInWhereClause() {
		assertQuery(
				"select e from IndexedEntity e where e.name <> 'Bob'",
				"MATCH (e:IndexedEntity) WHERE e.entityName <> \"Bob\" RETURN e" );
	}

	@Test
	public void shouldCreateQueryWithNestedNegationInWhereClause() {
		assertQuery(
				"select e from IndexedEntity e where NOT e.name <> 'Bob'",
				"MATCH (e:IndexedEntity) WHERE e.entityName = \"Bob\" RETURN e" );
	}

	@Test
	public void shouldCreateQueryUsingSelectWithConjunctionInWhereClause() {
		assertQuery(
				"select e from IndexedEntity e where e.title = 'same' and e.position = 1",
				"MATCH (e:IndexedEntity) WHERE (e.title = \"same\") AND (e.position = 1) RETURN e" );
	}

	@Test
	public void shouldCreateQueryWithNegationAndConjunctionInWhereClause() {
		assertQuery(
				"select e from IndexedEntity e where NOT ( e.name = 'Bob' AND e.position = 1 )",
				"MATCH (e:IndexedEntity) WHERE (e.entityName <> \"Bob\") OR (e.position <> 1) RETURN e" );
	}

	@Test
	public void shouldCreateNegatedRangeQuery() {
		assertQuery(
				"select e from IndexedEntity e where e.name = 'Bob' and not e.position between 1 and 3",
				"MATCH (e:IndexedEntity) WHERE (e.entityName = \"Bob\") AND (e.position < 1 OR e.position > 3) RETURN e" );
	}

	@Test
	public void shouldCreateBooleanQueryUsingSelect() {
		assertQuery(
				"select e from IndexedEntity e where e.name = 'same' or ( e.id = 4 and e.name = 'booh')",
				"MATCH (e:IndexedEntity) WHERE (e.entityName = \"same\") OR ((e.id = \"4\") AND (e.entityName = \"booh\")) RETURN e" );
	}

	@Test
	public void shouldCreateNumericBetweenQuery() {
		Map<String, Object> namedParameters = new HashMap<String, Object>();
		namedParameters.put( "lower", 10L );
		namedParameters.put( "upper", 20L );

		assertQuery(
				"select e from IndexedEntity e where e.position between :lower and :upper",
				namedParameters,
				"MATCH (e:IndexedEntity) WHERE e.position >= {lower} AND e.position <= {upper} RETURN e" );
	}

	@Test
	public void shouldCreateInQuery() {
		assertQuery(
				"select e from IndexedEntity e where e.title IN ( 'foo', 'bar', 'same')",
				"MATCH (e:IndexedEntity) WHERE ANY( _x_ IN [\"foo\", \"bar\", \"same\"] WHERE e.title = _x_) RETURN e" );
	}

	@Test
	public void shouldCreateNotInQuery() {
		assertQuery(
				"select e from IndexedEntity e where e.title NOT IN ( 'foo', 'bar', 'same')",
				"MATCH (e:IndexedEntity) WHERE NOT EXISTS(e.title) OR  NONE( _x_ IN [\"foo\", \"bar\", \"same\"] WHERE e.title = _x_) RETURN e" );
	}

	@Test
	public void shouldCreateLikeQuery() {
		assertQuery(
				"select e from IndexedEntity e where e.title like 'Ali_e%'",
				"MATCH (e:IndexedEntity) WHERE e.title=~\"^\\\\QAli\\\\E.\\\\Qe\\\\E.*$\" RETURN e" );
	}

	@Test
	public void shouldCreateNotLikeQuery() {
		assertQuery(
				"select e from IndexedEntity e where e.title not like 'Ali_e%'",
				"MATCH (e:IndexedEntity) WHERE NOT EXISTS(e.title) OR NOT(e.title=~\"^\\\\QAli\\\\E.\\\\Qe\\\\E.*$\") RETURN e" );
	}

	@Test
	public void shouldCreateIsNullQuery() {
		assertQuery(
				"select e from IndexedEntity e where e.title is null",
				"MATCH (e:IndexedEntity) WHERE NOT EXISTS(e.title) RETURN e" );
	}

	@Test
	public void shouldCreateIsNotNullQuery() {
		assertQuery(
				"select e from IndexedEntity e where e.title is not null",
				"MATCH (e:IndexedEntity) WHERE EXISTS(e.title) RETURN e" );
	}

	private void assertQuery(String hqlQuery, String expectedQuery) {
		assertQuery( hqlQuery, null, expectedQuery );
	}

	private void assertQuery(String hqlQuery, String expectedMongoDbQuery, Class<?> expectedEntityType) {
		assertQuery( hqlQuery, null, expectedMongoDbQuery, expectedEntityType );
	}

	private void assertQuery(String hqlQuery, Map<String, Object> namedParameters, String expectedQuery) {
		assertQuery( hqlQuery, namedParameters, expectedQuery, IndexedEntity.class );
	}

	private void assertQuery(String queryString, Map<String, Object> namedParameters, String expectedQuery, Class<?> expectedEntityType) {
		Neo4jQueryParsingResult parsingResult = parseQuery( queryString, namedParameters );
		assertThat( parsingResult ).isNotNull();
		assertThat( parsingResult.getEntityType() ).isSameAs( expectedEntityType );

		if ( expectedQuery == null ) {
			assertThat( parsingResult.getQueryObject() ).isNull();
		}
		else {
			assertThat( parsingResult.getQueryObject() ).isNotNull();
			assertThat( parsingResult.getQueryObject() ).isEqualTo( expectedQuery );
		}
	}

	private Neo4jQueryParsingResult parseQuery(String queryString) {
		return parseQuery( queryString, null );
	}

	private Neo4jQueryParsingResult parseQuery(String queryString, Map<String, Object> namedParameters) {
		return queryParser.parseQuery(
				queryString,
				setUpProcessingChain( namedParameters )
				);
	}

	private Neo4jProcessingChain setUpProcessingChain(Map<String, Object> namedParameters) {
		Map<String, Class<?>> entityNames = new HashMap<String, Class<?>>();
		entityNames.put( "com.acme.IndexedEntity", IndexedEntity.class );
		entityNames.put( "IndexedEntity", IndexedEntity.class );
		entityNames.put( "CommunityMemberST", CommunityMemberST.class );
		entityNames.put( "PersonST", PersonST.class );
		entityNames.put( "EmployeeST", EmployeeST.class );
		EntityNamesResolver nameResolver = new MapBasedEntityNamesResolver( entityNames );

		return new Neo4jProcessingChain( getSessionFactory(), nameResolver, namedParameters );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ IndexedEntity.class, PersonST.class, CommunityMemberST.class, EmployeeST.class };
	}
}
