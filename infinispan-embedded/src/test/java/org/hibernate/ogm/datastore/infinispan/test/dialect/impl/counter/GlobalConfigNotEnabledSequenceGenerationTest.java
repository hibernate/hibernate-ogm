/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.dialect.impl.counter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.utils.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * If the global state config is missing or disabled a validation error is issued.
 * Hibernate message id 1109. The global state config is mandatory for Storage.PERSISTENT counters.
 *
 * If the dialect needs to create runtime clustered counter, an Infinispan global configuration is mandatory.
 * In case of SequenceGenerator strategy the counters are created at start up.
 *
 * @author Fabio Massimo Ercoli
 * @see <a href="https://hibernate.atlassian.net/browse/OGM-1384">OGM-1384</a>
 * @see <a href="https://hibernate.atlassian.net/browse/OGM-1380">OGM-1380</a>
 */
@TestForIssue(jiraKey = { "OGM-1384", "OGM-1380" })
public class GlobalConfigNotEnabledSequenceGenerationTest extends StartAndCloseInfinispanEmbeddedFactoryBaseTest {

	private static final String SEQUENCE_NAME = "SEQUENCE";
	private static final int SEQUENCE_INITIAL_VALUE = 0;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	protected GridDialect dialect;

	@Test
	public void testExceptionWithSequencesGenerator() throws Exception {

		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM001109: Counter is not defined and cannot be created. Global persistent-location is missing in the Infinispan configuration" );

		startAndCloseFactory( GlobalConfigNotEnabledSequenceGenerationTest.EntityWithSequence.class, "infinispan-dist-noglobal.xml" );
	}

	@Entity
	@Table(name = "SEQUENCE_GENERATOR")
	private static class EntityWithSequence {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
		@SequenceGenerator(name = "gen", sequenceName = SEQUENCE_NAME, initialValue = SEQUENCE_INITIAL_VALUE)
		Long id;
	}

}
