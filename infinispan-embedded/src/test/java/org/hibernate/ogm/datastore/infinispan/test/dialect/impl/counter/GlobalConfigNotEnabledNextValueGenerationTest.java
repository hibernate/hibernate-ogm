/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.dialect.impl.counter;

import static org.fest.assertions.Fail.fail;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.ogm.datastore.infinispan.InfinispanProperties;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.id.impl.OgmSequenceGenerator;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * If the global state config is missing or disabled a validation error is issued.
 * Hibernate message id 1109. The global state config is mandatory for Storage.PERSISTENT counters.
 *
 * @author Fabio Massimo Ercoli
 * @see <a href="https://hibernate.atlassian.net/browse/OGM-1376">OGM-1384</a>
 */
@TestForIssue(jiraKey = { "OGM-1384" })
public class GlobalConfigNotEnabledNextValueGenerationTest extends OgmJpaTestCase {

	private static final String SEQUENCE_NAME = "SEQUENCE";
	private static final String TABLE_ROW_KEY_NAME = "TABLE_ROW_KEY";
	private static final int SEQUENCE_INITIAL_VALUE = 0;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	protected GridDialect dialect;

	@Override
	protected void configure(GetterPersistenceUnitInfo info) {
		info.getProperties().setProperty( InfinispanProperties.CONFIGURATION_RESOURCE_NAME, "infinispan-dist-noglobal.xml" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { GlobalConfigNotEnabledNextValueGenerationTest.EntityWithSequence.class,
			GlobalConfigNotEnabledNextValueGenerationTest.EntityWithTableGenerator.class };
	}

	@Before
	public void setUp() {
		ServiceRegistryImplementor serviceRegistry = getServiceRegistry();
		dialect = serviceRegistry.getService( GridDialect.class );
	}

	@Test
	public void testExceptionWithSequencesGenerator() throws Exception {

		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM001109: Counter is not defined and cannot be created. Global persistent-location is missing in the Infinispan configuration" );

		SessionFactoryImplementor sessionFactory = getFactory().unwrap( SessionFactoryImplementor.class );
		IdentifierGenerator generator = sessionFactory.getIdentifierGenerator( GlobalConfigNotEnabledNextValueGenerationTest.EntityWithSequence.class.getName() );
		IdSourceKeyMetadata sequenceMetadata = ( (OgmSequenceGenerator) generator ).getGeneratorKeyMetadata();
		IdSourceKey generatorKey = IdSourceKey.forTable( sequenceMetadata, "SEQUENCE" );

		dialect.nextValue( new NextValueRequest( generatorKey, 1, 0 ) );
		fail( "expected exception was not raised" );

	}

	@Test
	public void testExceptionWithTableGenerator() throws Exception {

		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM001109: Counter is not defined and cannot be created. Global persistent-location is missing in the Infinispan configuration" );

		SessionFactoryImplementor sessionFactory = getFactory().unwrap( SessionFactoryImplementor.class );
		IdentifierGenerator generator = sessionFactory.getIdentifierGenerator( GlobalConfigNotEnabledNextValueGenerationTest.EntityWithSequence.class.getName() );
		IdSourceKeyMetadata tableMetadata = ( (OgmSequenceGenerator) generator ).getGeneratorKeyMetadata();
		IdSourceKey generatorKey = IdSourceKey.forTable( tableMetadata, TABLE_ROW_KEY_NAME );

		dialect.nextValue( new NextValueRequest( generatorKey, 1, 0 ) );
		fail( "expected exception was not raised" );

	}

	@Entity
	@Table(name = "SEQUENCE_GENERATOR")
	private static class EntityWithSequence {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
		@SequenceGenerator(name = "gen", sequenceName = SEQUENCE_NAME, initialValue = SEQUENCE_INITIAL_VALUE)
		Long id;
	}

	@Entity
	@Table(name = "TABLE_GENERATOR")
	private static class EntityWithTableGenerator {

		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "gen2")
		@TableGenerator(name = "gen2", initialValue = SEQUENCE_INITIAL_VALUE, pkColumnValue = TABLE_ROW_KEY_NAME)
		Long id;
	}

}
