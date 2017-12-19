/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl;

import org.hibernate.ogm.datastore.infinispan.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispan.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.common.externalizer.impl.ExternalizerIds;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.common.externalizer.impl.RowKeyExternalizer;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.externalizer.impl.AssociationKeyExternalizer;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.externalizer.impl.EntityKeyExternalizer;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.externalizer.impl.EntityKeyMetadataExternalizer;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.externalizer.impl.IdSourceKeyExternalizer;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl.PersistentAssociationKeyExternalizer;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl.PersistentEntityKeyExternalizer;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl.PersistentIdSourceKeyExternalizer;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.SerializationConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Helpers to ensure registration of all custom Externalizers we need for Hibernate OGM / Infinispan.
 *
 * @author Sanne Grinovero
 */
public final class ExternalizersIntegration {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );
	private static final Map<Integer, AdvancedExternalizer<?>> ogmExternalizers = initializeOgmExternalizers();

	private ExternalizersIntegration() {
		// to not be created
	}

	private static Map<Integer, AdvancedExternalizer<?>> initializeOgmExternalizers() {
		Map<Integer, AdvancedExternalizer<?>> m = new HashMap<>();
		//Register here any new Externalizer that we might need:
		addExternalizerToMap( m, AssociationKeyExternalizer.INSTANCE );
		addExternalizerToMap( m, EntityKeyExternalizer.INSTANCE );
		addExternalizerToMap( m, EntityKeyMetadataExternalizer.INSTANCE );
		addExternalizerToMap( m, IdSourceKeyExternalizer.INSTANCE );
		addExternalizerToMap( m, PersistentAssociationKeyExternalizer.INSTANCE );
		addExternalizerToMap( m, PersistentEntityKeyExternalizer.INSTANCE );
		addExternalizerToMap( m, PersistentIdSourceKeyExternalizer.INSTANCE );
		addExternalizerToMap( m, RowKeyExternalizer.INSTANCE );
		return Collections.unmodifiableMap( m );
	}

	private static void addExternalizerToMap(Map<Integer, AdvancedExternalizer<?>> m, AdvancedExternalizer<?> instance) {
		Integer id = instance.getId();
		assert id != null;
		//The range of ids assigned to Hibernate OGM by the Infinispan team needs to be strictly verified;
		//see http://infinispan.org/docs/9.0.x/user_guide/user_guide.html#preassigned_externalizer_id_ranges
		//and please use org.hibernate.ogm.datastore.infinispan.persistencestrategy.common.externalizer.impl.ExternalizerIds
		assert id.intValue() >= 1400 && id.intValue() < 1500 : "Externalizer out of the assigned range for Hibernate OGM";
		AdvancedExternalizer<?> previous = m.put( instance.getId(), instance );
		assert previous == null : "Clash in Externalizer Id! They need to be strictly unique.";
	}

	/**
	 * Registers all custom Externalizer implementations that Hibernate OGM needs into an Infinispan CacheManager
	 * configuration.
	 *
	 * @see ExternalizerIds
	 * @param cfg the Serialization section of a GlobalConfiguration builder
	 */
	public static void registerOgmExternalizers(SerializationConfigurationBuilder cfg) {
		for ( AdvancedExternalizer<?> advancedExternalizer : ogmExternalizers.values() ) {
			cfg.addAdvancedExternalizer( advancedExternalizer );
		}
	}

	/**
	 * Registers all custom Externalizer implementations that Hibernate OGM needs into a running
	 * Infinispan CacheManager configuration.
	 * This is only safe to do when Caches from this CacheManager haven't been started yet,
	 * or the ones already started do not contain any data needing these.
	 *
	 * @see ExternalizerIds
	 * @param globalCfg the Serialization section of a GlobalConfiguration builder
	 */
	public static void registerOgmExternalizers(GlobalConfiguration globalCfg) {
		Map<Integer, AdvancedExternalizer<?>> externalizerMap = globalCfg.serialization().advancedExternalizers();
		externalizerMap.putAll( ogmExternalizers );
	}

	/**
	 * Verify that all OGM custom externalizers are present.
	 * N.B. even if some Externalizer is only needed in specific configuration,
	 * it is not safe to start a CacheManager without one as the same CacheManager
	 * might be used, or have been used in the past, to store data using a different
	 * configuration.
	 *
	 * @see ExternalizerIds
	 * @see AdvancedExternalizer
	 * @param externalCacheManager the provided CacheManager to validate
	 */
	public static void validateExternalizersPresent(EmbeddedCacheManager externalCacheManager) {
		Map<Integer, AdvancedExternalizer<?>> externalizerMap = externalCacheManager
				.getCacheManagerConfiguration()
				.serialization()
				.advancedExternalizers();
		for ( AdvancedExternalizer<?> ogmExternalizer : ogmExternalizers.values() ) {
			final Integer externalizerId = ogmExternalizer.getId();
			AdvancedExternalizer<?> registeredExternalizer = externalizerMap.get( externalizerId );
			if ( registeredExternalizer == null ) {
				throw log.externalizersNotRegistered( externalizerId, ogmExternalizer.getClass() );
			}
			else if ( !registeredExternalizer.getClass().equals( ogmExternalizer ) ) {
				if ( registeredExternalizer.getClass().toString().equals( ogmExternalizer.getClass().toString() ) ) {
					// same class name, yet different Class definition!
					throw log.registeredExternalizerNotLoadedFromOGMClassloader( registeredExternalizer.getClass() );
				}
				else {
					throw log.externalizerIdNotMatchingType( externalizerId, registeredExternalizer, ogmExternalizer );
				}
			}
		}
	}

}
