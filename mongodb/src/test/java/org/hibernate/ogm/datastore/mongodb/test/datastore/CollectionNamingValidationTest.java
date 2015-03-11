/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.datastore;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.MappingException;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.Assert;
import org.junit.Test;

/**
 * Makes sure generated collection names are validated according to
 * the restrictions imposed by MongoDB collection names.
 *
 * @author Sanne Grinovero
 */
@TestForIssue(jiraKey = "OGM-265")
public class CollectionNamingValidationTest {

	@Test
	public void systemPrefixedTableNameIsIllegal() {
		assertTableCausesException( SystemNamedTable.class, "OGM001220" );
	}

	@Test
	public void nulTableNameIsIllegal() {
		assertTableCausesException( NULNamedTable.class, "OGM001221" );
	}

	@Test
	public void dollarTableNameIsIllegal() {
		assertTableCausesException( DollarNamedTable.class, "OGM001222" );
	}

	@Test
	public void dollarColumnNameIsIllegal() {
		assertTableCausesException( InvalidColumnsTable.class, "OGM001223" );
	}

	@Test
	public void defaultInnerClassNameIsIllegal() {
		assertTableCausesException( DollarNamedTable.class, "OGM001222" );
	}

	private void assertTableCausesException(Class<?> mappedType, String expectedExceptionPrefix) {
		OgmConfiguration configuration = TestHelper.getDefaultTestConfiguration( mappedType );
		try {
			OgmSessionFactory sessionFactory = configuration.buildSessionFactory();
			sessionFactory.close();
			Assert.fail( "An exception was expected" );
		}
		catch (MappingException me) {
			assertThat( me.getMessage() ).startsWith( expectedExceptionPrefix );
		}
	}

	@Entity
	public static class EmptyNamedTable {
		@Id Long id;
	}

	@Entity @Table(name = "system.blue.pill")
	public static class SystemNamedTable {
		@Id Long id;
	}

	@Entity @Table(name = "blah\0")
	public static class NULNamedTable {
		@Id Long id;
	}

	@Entity @Table(name = "blah$0")
	public static class DollarNamedTable {
		@Id Long id;
	}

	@Entity @Table(name = "valid")
	public static class InvalidColumnsTable {
		@Id Long id;
		@Column(name = "$DOLLARS") String field;
	}

}
