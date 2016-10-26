/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.utils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndexType;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgnitionEx;
import org.apache.ignite.internal.binary.BinaryMarshaller;
import org.apache.ignite.logger.slf4j.Slf4jLogger;

/**
 *
 * @author Victor Kadachigov
 */
public class IgniteTestConfigurationBuilder {


	public IgniteConfiguration build() {

		IgniteConfiguration config = null;

		try {
			//config = loadFromResource( "ignite-config.xml" );
			config = createConfig();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}

		return config;
	}

	private IgniteConfiguration loadFromResource(String resourceName) throws IgniteCheckedException {
		URL url = getClass().getClassLoader().getResource( resourceName );
		return IgnitionEx.loadConfiguration( url ).get1();
	}

	private IgniteConfiguration createConfig() {
		IgniteConfiguration config = new IgniteConfiguration();
		
		config.setGridName( "OgmTestGrid" );
		config.setLocalHost( "127.0.0.1" );
		config.setPeerClassLoadingEnabled( true );
		config.setClientMode( false );
		config.setMarshaller( new BinaryMarshaller() );
		config.setGridLogger( new Slf4jLogger() );
		config.setPublicThreadPoolSize( 2 );
		
		List<CacheConfiguration> cacheConfig = new ArrayList<>(); 
// SequenceIdGeneratorTest
		cacheConfig.add( createCacheConfig( "Song" ) );
		cacheConfig.add( createCacheConfig( "Actor" ) );
// JPAResourceLocalTest
		cacheConfig.add( createCacheConfig( "Poem" ) );
// ManyToManyTest		
		cacheConfig.add( createCacheConfig( "Car" ) );
		cacheConfig.add( createCacheConfig( "Tire" ) );
		cacheConfig.add( createCacheConfig( "Car_Tire" ) );
		cacheConfig.add( createCacheConfig( "AccountOwner" ) );
		cacheConfig.add( createCacheConfig( "BankAccount" ) );
		cacheConfig.add( createCacheConfig( "AccountOwner_BankAccount" ) );
// ListTest
		cacheConfig.add( createCacheConfig( "Child", "Father_id" ) ); 
		cacheConfig.add( createCacheConfig( "Father" ) );
		cacheConfig.add( createCacheConfig( "Father_child", "Father_id" ) );
		cacheConfig.add( createCacheConfig( "GrandChild" ) );
		cacheConfig.add( createCacheConfig( "GrandMother", "id" ) );
		cacheConfig.add( createCacheConfig( "GrandMother_grandChildren", "GrandMother_id" ) );
		cacheConfig.add( createCacheConfig( "Race" ) );
		cacheConfig.add( createCacheConfig( "Runner" ) );
		cacheConfig.add( createCacheConfig( "Race_Runners", "Race_raceId" ) );
// ManyToManyExtraTest
		cacheConfig.add( createCacheConfig( "Student" ) );
		cacheConfig.add( createCacheConfig( "ClassRoom" ) );
		cacheConfig.add( createCacheConfig( "ClassRoom_Student" ) );
// MapTest
		
		config.setCacheConfiguration(cacheConfig.toArray( new CacheConfiguration[cacheConfig.size()] ));
		
		return config;
	}
	
	private CacheConfiguration createCacheConfig( String name, String... indexedFields ) {
		CacheConfiguration<String, BinaryObject> result = new CacheConfiguration<>();
		
		result.setAtomicityMode( CacheAtomicityMode.TRANSACTIONAL );
		result.setCacheMode( CacheMode.PARTITIONED );
		result.setStartSize( 10 );
		result.setBackups( 0 );
		result.setAffinity( new RendezvousAffinityFunction( false, 10 ) );
		result.setName(name);
		QueryEntity queryEntity = new QueryEntity( String.class.getName(), name );
		if ( indexedFields != null ) {
			for (String field : indexedFields) {
				queryEntity.ensureIndex( field, QueryIndexType.SORTED );
			}
		}
		result.setQueryEntities( Arrays.asList( queryEntity ) );
		
		return result;
	}

}
