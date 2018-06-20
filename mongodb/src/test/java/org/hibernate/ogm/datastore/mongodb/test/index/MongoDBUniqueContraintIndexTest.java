/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.index;

import static org.fest.assertions.Assertions.assertThat;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;

import org.junit.Test;

/**
 * Tests it is possible to define on the same field of a given Entity,
 * both a {@link UniqueConstraint} and an {@link Index}.
 *
 * @author Fabio Massimo Ercoli
 */
@TestForIssue(jiraKey = "OGM-1496")
public class MongoDBUniqueContraintIndexTest extends OgmTestCase {

	@Entity
	@Table(name = "I_POLICYHOLDER", uniqueConstraints = @UniqueConstraint(columnNames = "email"),
			indexes = { @Index(columnList = "email", name = "I_POLICYHOLDER_EMAIL") })
	static class PolicyHolder {

		@Id
		private String name;
		private String email;
		private String address;

		public String getEmail() {
			return email;
		}

		public String getAddress() {
			return address;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { PolicyHolder.class };
	}

	@Test
	public void testUniqueAndIndexOnTheSameField() {
		PolicyHolder policyHolder = new PolicyHolder();
		policyHolder.name = "Fabio Massimo";
		policyHolder.email = "fabiomassimo@myemailprovider.eu";
		policyHolder.address = "11bis Rue Roquepine Paris";

		inTransaction( session -> {
			session.persist( policyHolder );
		} );

		inTransaction( session -> {
			PolicyHolder stored = session.load( PolicyHolder.class, policyHolder.name );
			assertThat( policyHolder.email ).isEqualTo( stored.getEmail() );
			assertThat( policyHolder.address ).isEqualTo( stored.getAddress() );
		} );

		deleteAll( PolicyHolder.class, policyHolder.name );
		checkCleanCache();
	}
}
