/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import static org.fest.assertions.Assertions.assertThat;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.hibernate.id.IdentifierGenerator;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.id.impl.OgmTableGenerator;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.junit.Test;

/**
 * Test what happens if two table generator in two different entities using different tables have the same name.
 *
 * @author Davide D'Alto
 */
public class TableNextValueTableNameTest extends TestNextValueGeneration {

	private static final String SEQUENCE_NAME = "generator";

	private static final int INITIAL_VALUE = 5;

	@Override
	protected IdSourceKey buildIdGeneratorKey(Class<?> entityClass, String sequenceName) {
		IdentifierGenerator metadata = generateKeyMetadata( entityClass );
		IdSourceKeyMetadata tableMetadata = ( (OgmTableGenerator) metadata ).getGeneratorKeyMetadata();
		return IdSourceKey.forTable( tableMetadata, sequenceName );
	}

	@Test
	public void testThereAreNoConflicts() {
		Number rubyNextValue = nextValue( Ruby.class );
		Number pearlNextValue = nextValue( Pearl.class );

		assertThat( rubyNextValue ).as( "Should be the same because the generators are stored in different tables, even if they have the same pkColumnName" )
				.isEqualTo( pearlNextValue );
	}

	private Number nextValue(Class<?> entityClass) {
		final IdSourceKey generatorKey = buildIdGeneratorKey( entityClass, SEQUENCE_NAME );
		NextValueRequest request = new NextValueRequest( generatorKey, 1, INITIAL_VALUE );
		Number nextValue = dialect.nextValue( request );
		return nextValue;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Ruby.class, Pearl.class };
	}

	@Entity
	@Table(name = "Ruby")
	private static class Ruby {

		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "gen2")
		@TableGenerator(name = "gen2", table = "Ruby_generators", initialValue = INITIAL_VALUE, pkColumnValue = SEQUENCE_NAME)
		Long id;
	}

	@Entity
	@Table(name = "Pearl")
	private static class Pearl {

		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "gen3")
		@TableGenerator(name = "gen3", table = "Pearl_generator", initialValue = INITIAL_VALUE, pkColumnValue = SEQUENCE_NAME)
		Long id;
	}
}
