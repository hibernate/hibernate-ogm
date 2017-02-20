/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.EMBEDDED;
import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.test.dsl.GraphAssertions.node;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
@TestForIssue(jiraKey = "OGM-1253")
public class MapWithEmbeddedTest extends Neo4jJpaTestCase {

	private ForumUser user;
	private JiraIssue ogmIssue;
	// we should also test the use case where all properties in JiraIssue are null

	@Before
	public void prepareDb() throws Exception {
		final EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		ogmIssue = new JiraIssue( 123, "OGM" );

		user = new ForumUser( "user123" );
		user.getIssues().put( "issue", ogmIssue );

		em.persist( user );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		NodeForGraphAssertions userNode = node( "user", ForumUser.LABEL, ENTITY.name() )
				.property( "id", user.getId() );

		/*
		 * The mapping at the moment is not what one would expect, it will change in 5.2
		 */
		NodeForGraphAssertions issueNode = node( "issue", "MapWithEmbeddedTest$ForumUser_issues", EMBEDDED.name() );

		NodeForGraphAssertions issueValueNode = node( "issueValue", EMBEDDED.name() )
				.property( "number", ogmIssue.getNumber() )
				.property( "project", ogmIssue.getProject() );

		assertThatOnlyTheseNodesExist( userNode, issueValueNode, issueNode );

		RelationshipsChainForGraphAssertions relationships1 = userNode.relationshipTo( issueNode, "issues" ).property( "issues_KEY", "issue" )
				.relationshipTo( issueValueNode, "value" );

		assertThatOnlyTheseRelationshipsExist( relationships1 );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ ForumUser.class };
	}

	@Entity
	@Table(name = ForumUser.LABEL)
	@SuppressWarnings("unused")
	private static class ForumUser {

		public static final String LABEL = "FORUM_USER";

		@Id
		private String id;

		@ElementCollection
		private Map<String, JiraIssue> issues = new HashMap<>();

		public ForumUser() {
		}

		public ForumUser(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Map<String, JiraIssue> getIssues() {
			return issues;
		}

		public void setIssues(Map<String, JiraIssue> issues) {
			this.issues = issues;
		}
	}

	@Embeddable
	@Table(name = JiraIssue.LABEL)
	@SuppressWarnings("unused")
	private static class JiraIssue {

		public static final String LABEL = "JIRA_ISSUE";

		private Integer number;
		private String project;

		public JiraIssue() {
		}

		public JiraIssue(Integer number, String project) {
			this.number = number;
			this.project = project;
		}

		public String getProject() {
			return project;
		}

		public void setProject(String project) {
			this.project = project;
		}

		public Integer getNumber() {
			return number;
		}

		public void setNumber(Integer number) {
			this.number = number;
		}

		@Override
		public String toString() {
			return project + "-" + number;
		}
	}
}
