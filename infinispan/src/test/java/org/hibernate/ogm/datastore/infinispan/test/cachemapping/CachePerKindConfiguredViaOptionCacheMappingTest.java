/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.cachemapping;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Map;

import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.cfg.Configurable;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.cfg.OptionConfigurator;
import org.hibernate.ogm.datastore.infinispan.Infinispan;
import org.hibernate.ogm.datastore.keyvalue.options.CacheMappingType;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.infinispan.Cache;
import org.junit.Test;

/**
 * Test for the {@link org.hibernate.ogm.datastore.keyvalue.options.CacheMappingType#CACHE_PER_KIND} strategy, given through the option system.
 *
 * @author Gunnar Morling
 */
public class CachePerKindConfiguredViaOptionCacheMappingTest extends CacheMappingTestBase {

	@Test
	public void shouldUseCachePerTable() {
		Cache<?, Map<String, Object>> plantCache = getEntityCache( "Plant", "id" );
		assertThat( plantCache.getName() ).isEqualTo( "ENTITIES" );

		Cache<?, Map<String, Object>> familyCache = getEntityCache( "Family", "id" );
		assertThat( familyCache.getName() ).isEqualTo( "ENTITIES" );

		Cache<?, Map<RowKey, Map<String, Object>>> membersCache = getAssociationCache( "Family_Plant", "Family_id" );
		assertThat( membersCache.getName() ).isEqualTo( "ASSOCIATIONS" );

		Cache<?, Object> plantSequenceCache = getIdSourceCache( "hibernate_sequences" );
		assertThat( plantSequenceCache.getName() ).isEqualTo( "IDENTIFIERS" );
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.getProperties().put( OgmProperties.OPTION_CONFIGURATOR, new OptionConfigurator() {

			@Override
			public void configure(Configurable configurable) {
				configurable.configureOptionsFor( Infinispan.class )
					.cacheMapping( CacheMappingType.CACHE_PER_KIND );
			}
		} );
	}
}
