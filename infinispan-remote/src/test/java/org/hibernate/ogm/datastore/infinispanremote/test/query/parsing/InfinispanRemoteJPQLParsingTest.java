/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.query.parsing;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;

import org.hibernate.ogm.datastore.infinispanremote.query.impl.InfinispanRemoteQueryDescriptor;
import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.InfinispanRemoteBasedQueryParserService;
import org.hibernate.ogm.datastore.infinispanremote.utils.InfinispanRemoteServerRunner;
import org.hibernate.ogm.query.spi.QueryParsingResult;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration test scoped to the parsing process
 * of a JPQL query into an Infinispan Server query
 *
 * @author Fabio Massimo Ercoli &lt;fabio@hibernate.org&gt;
 */
@RunWith(InfinispanRemoteServerRunner.class)
public class InfinispanRemoteJPQLParsingTest extends OgmTestCase {

	private InfinispanRemoteBasedQueryParserService testTarget = new InfinispanRemoteBasedQueryParserService();

	@Test
	public void shouldCreateUnrestrictedQuery() {
		verifyParsing( "from " + IndexedEntity.class.getName(),
				"IndexedEntity", "from HibernateOGMGenerated.IndexedEntity"
		);
	}

	@Test
	public void shouldCreateQueryWithSingleDiscriminatorValue() {
		verifyParsing( "from " + EmployeeST.class.getName(),
				"PersonST", "from HibernateOGMGenerated.PersonST where DTYPE = 'EMP'"
		);
	}

	@Test
	public void shouldCreateQueryWithSingleDiscriminatorValueWithFilter() {
		verifyParsing( "from " + EmployeeST.class.getName() + " e where e.employer = 'Red Hat'",
				"PersonST", "from HibernateOGMGenerated.PersonST where employer = 'Red Hat' and DTYPE = 'EMP'"
		);
	}

	@Test
	public void shouldCreateQueryWithMultipleDiscriminatorValues() {
		verifyParsing( "from " + CommunityMemberST.class.getName(),
				"PersonST", "from HibernateOGMGenerated.PersonST where DTYPE in ('EMP', 'CMM')"
		);
	}

	@Test
	public void shouldCreateQueryWithMultipleDiscriminatorValuesWithFilter() {
		verifyParsing( "from " + CommunityMemberST.class.getName() + " c where c.project = 'Hibernate OGM'",
				"PersonST", "from HibernateOGMGenerated.PersonST where project = 'Hibernate OGM' and DTYPE in ('EMP', 'CMM')"
		);
	}

	@Test
	public void shouldCreateRestrictedQueryUsingSelect() {
		verifyParsing( "select e from " + IndexedEntity.class.getName() + " e where e.title = 'same'",
				"IndexedEntity", "from HibernateOGMGenerated.IndexedEntity where title = 'same'"
		);
	}

	@Test
	public void shouldUseSpecialNameForIdPropertyInWhereClause() {
		verifyParsing( "select e from " + IndexedEntity.class.getName() + " e where e.id = '1'",
				"IndexedEntity", "from HibernateOGMGenerated.IndexedEntity where id = '1'"
		);
	}

	@Test
	public void shouldUseColumnNameForPropertyInWhereClause() {
		verifyParsing( "select e from " + IndexedEntity.class.getName() + " e where e.name = 'Bob'",
				"IndexedEntity", "from HibernateOGMGenerated.IndexedEntity where entityName = 'Bob'"
		);
	}

	@Test
	public void shouldCreateProjectionQuery() {
		verifyParsing( "select e.id, e.name, e.position from " + IndexedEntity.class.getName() + " e",
				"IndexedEntity", "select id, entityName, position from HibernateOGMGenerated.IndexedEntity",
				"id", "entityName", "position"
		);
	}

	@Test
	public void shouldAddNumberPropertyAsNumber() {
		verifyParsing( "select e from " + IndexedEntity.class.getName() + " e where e.position = 2",
				"IndexedEntity", "from HibernateOGMGenerated.IndexedEntity where position = 2"
		);
	}

	@Test
	public void shouldCreateLessOrEqualQuery() {
		verifyParsing( "select e from " + IndexedEntity.class.getName() + " e where e.position <= 20",
				"IndexedEntity", "from HibernateOGMGenerated.IndexedEntity where position <= 20"
		);
	}

	@Test
	public void shouldCreateQueryWithNegationInWhereClause() {
		verifyParsing( "select e from " + IndexedEntity.class.getName() + " e where e.name <> 'Bob'",
				"IndexedEntity", "from HibernateOGMGenerated.IndexedEntity where entityName <> 'Bob'"
		);
	}

	@Test
	public void shouldCreateQueryWithNestedNegationInWhereClause() {
		verifyParsing( "select e from " + IndexedEntity.class.getName() + " e where NOT e.name <> 'Bob'",
				"IndexedEntity", "from HibernateOGMGenerated.IndexedEntity where entityName = 'Bob'"
		);
	}

	@Test
	public void shouldCreateQueryUsingSelectWithConjunctionInWhereClause() {
		verifyParsing( "select e from " + IndexedEntity.class.getName() + " e where e.title = 'same' and e.position = 1",
				"IndexedEntity", "from HibernateOGMGenerated.IndexedEntity where (title = 'same') and (position = 1)"
		);
	}

	@Test
	public void shouldCreateQueryWithNegationAndConjunctionInWhereClause() {
		verifyParsing( "select e from " + IndexedEntity.class.getName() + " e where NOT ( e.name = 'Bob' AND e.position = 1 )",
				"IndexedEntity", "from HibernateOGMGenerated.IndexedEntity where (entityName <> 'Bob') or (position <> 1)"
		);
	}

	@Test
	public void shouldCreateNegatedRangeQuery() {
		verifyParsing( "select e from " + IndexedEntity.class.getName() + " e where e.name = 'Bob' and not e.position between 1 and 3",
				"IndexedEntity", "from HibernateOGMGenerated.IndexedEntity where (entityName = 'Bob') and (( position < 1 || position > 3 ))"
		);
	}

	@Test
	public void shouldCreateBooleanQueryUsingSelect() {
		verifyParsing( "select e from " + IndexedEntity.class.getName() + " e where e.name = 'same' or ( e.id = 4 and e.name = 'booh')",
				"IndexedEntity", "from HibernateOGMGenerated.IndexedEntity where (entityName = 'same') or ((id = '4') and (entityName = 'booh'))"
		);
	}

	@Test
	public void shouldCreateNumericBetweenQuery() {
		verifyParsing( "select e from " + IndexedEntity.class.getName() + " e where e.position between :lower and :upper",
				"IndexedEntity", "from HibernateOGMGenerated.IndexedEntity where ( position >= :lower && position <= :upper )"
		);
	}

	@Test
	public void shouldCreateInQuery() {
		verifyParsing( "select e from " + IndexedEntity.class.getName() + " e where e.title IN ( 'foo', 'bar', 'same')",
				"IndexedEntity", "from HibernateOGMGenerated.IndexedEntity where title in ('foo', 'bar', 'same')"
		);
	}

	@Test
	public void shouldCreateNotInQuery() {
		verifyParsing( "select e from " + IndexedEntity.class.getName() + " e where e.title NOT IN ( 'foo', 'bar', 'same')",
				"IndexedEntity", "from HibernateOGMGenerated.IndexedEntity where not title in ('foo', 'bar', 'same')"
		);
	}

	@Test
	public void shouldCreateLikeQuery() {
		verifyParsing( "select e from " + IndexedEntity.class.getName() + " e where e.title like 'Ali_e%'",
				"IndexedEntity", "from HibernateOGMGenerated.IndexedEntity where title LIKE 'Ali_e%'"
		);
	}

	@Test
	public void shouldCreateNotLikeQuery() {
		verifyParsing( "select e from " + IndexedEntity.class.getName() + " e where e.title not like 'Ali_e%'",
				"IndexedEntity", "from HibernateOGMGenerated.IndexedEntity where not title LIKE 'Ali_e%'"
		);
	}

	@Test
	public void shouldCreateIsNullQuery() {
		verifyParsing( "select e from " + IndexedEntity.class.getName() + " e where e.title is null",
				"IndexedEntity", "from HibernateOGMGenerated.IndexedEntity where title is null"
		);
	}

	@Test
	public void shouldCreateIsNotNullQuery() {
		verifyParsing( "select e from " + IndexedEntity.class.getName() + " e where e.title is not null",
				"IndexedEntity", "from HibernateOGMGenerated.IndexedEntity where title is not null"
		);
	}

	private void verifyParsing(String jpql, String cache, String datastoreQuery, String... projections) {
		// To *QueryParserService internal SPI the JPQL query arrives always
		// with the Entity name expresses as **fully qualified** class name,
		// even if in the original API invocation it had been expressed as a **simple name**.
		// For this reason the queries used in this test are always like:
		// %org.hibernate.ogm.datastore.infinispanremote.test.query.parsing.IndexedEntity%
		// ans never like: %IndexedEntity%.
		QueryParsingResult queryParsingResult = testTarget.parseQuery( getSessionFactory(), jpql );
		InfinispanRemoteQueryDescriptor queryDescriptor = (InfinispanRemoteQueryDescriptor) queryParsingResult.getQueryObject();

		assertThat( queryParsingResult.getColumnNames() ).isEqualTo( Arrays.asList( projections ) );
		assertThat( queryDescriptor.getCache() ).isEqualTo( cache );
		assertThat( queryDescriptor.getQuery() ).isEqualTo( datastoreQuery );
		assertThat( queryDescriptor.getProjections() ).isEqualTo( projections );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				IndexedEntity.class, PersonST.class, CommunityMemberST.class, EmployeeST.class
		};
	}
}
