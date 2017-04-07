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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.cache.QueryIndexType;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.configuration.BinaryConfiguration;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.TransactionConfiguration;
import org.apache.ignite.internal.IgnitionEx;
import org.apache.ignite.internal.binary.BinaryMarshaller;
import org.apache.ignite.logger.slf4j.Slf4jLogger;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;
import org.hibernate.ogm.datastore.ignite.IgniteConfigurationBuilder;

/**
 * Ignite cache configuration for tests
 *
 * @author Victor Kadachigov
 */
public class IgniteTestConfigurationBuilder implements IgniteConfigurationBuilder {

	@Override
	public IgniteConfiguration build() {
		//disable check for new versions
		System.setProperty( IgniteSystemProperties.IGNITE_UPDATE_NOTIFIER, Boolean.FALSE.toString() );

		IgniteConfiguration config = null;

		try {
			//config = loadFromResource( "ignite-config.xml" );
			config = createConfig();
		}
		catch (Exception ex) {
			throw new RuntimeException( ex );
		}

		return config;
	}

	private IgniteConfiguration loadFromResource(String resourceName) throws IgniteCheckedException {
		URL url = getClass().getClassLoader().getResource( resourceName );
		return IgnitionEx.loadConfiguration( url ).get1();
	}

	private IgniteConfiguration createConfig() {
		IgniteConfiguration config = new IgniteConfiguration();

		config.setLocalHost( "127.0.0.1" );
		config.setPeerClassLoadingEnabled( true );
		config.setClientMode( false );
		config.setMarshaller( new BinaryMarshaller() ) ;
		BinaryConfiguration binaryConfiguration = new BinaryConfiguration();
		binaryConfiguration.setCompactFooter( false );		// it is necessary only for embedded collections (@ElementCollection)
		config.setBinaryConfiguration( binaryConfiguration );
		config.setGridLogger( new Slf4jLogger() );
		config.setPublicThreadPoolSize( 2 );
		TransactionConfiguration transactionConfiguration = new TransactionConfiguration();
		transactionConfiguration.setDefaultTxConcurrency( TransactionConcurrency.PESSIMISTIC );
		transactionConfiguration.setDefaultTxIsolation( TransactionIsolation.READ_COMMITTED );
		config.setTransactionConfiguration( transactionConfiguration );

		List<CacheConfiguration> cacheConfig = new ArrayList<>();

// ManyToManyTest
		cacheConfig.add(
				createCacheConfig( "Car_Tire" )
					.appendIndex( "cars_carId_maker", String.class )
					.appendIndex( "cars_carId_model", String.class )
					.appendIndex( "tires_tireId_maker", String.class )
					.appendIndex( "tires_tireId_model", String.class )
					.build()
		);
		cacheConfig.add(
				createCacheConfig( "AccountOwner_BankAccount" )
					.appendIndex( "owners_id", String.class )
					.appendIndex( "bankAccounts_id", String.class )
					.build()
		);
// ListTest
		cacheConfig.add(
				createCacheConfig( "Race_Runners" )
						.appendIndex( "Race_raceId_federationDepartment", Integer.class )
						.appendIndex( "Race_raceId_federationSequence", Integer.class )
						.build()
		);
// ManyToOneTest
		cacheConfig.add(
				createCacheConfig( "Game" )
						.withKeyType( "GameId" )
						.appendIndex( "playedOn_id_countryCode", String.class )
						.appendIndex( "playedOn_id_sequenceNo", Integer.class )
						.build()
		);
// CompositeIdTest
		cacheConfig.add(
				createCacheConfig( "Label" )
						.withKeyType( Long.class )
						.appendIndex( "news_author_fk", String.class )
						.appendIndex( "news_topic_fk", String.class )
						.build()
		);
// EmbeddableExtraTest
		cacheConfig.add( simpleCacheConfig( "Order" ) );  // Order is reserved word in SQL. Can't create QueryEntry class
// SimpleQueriesTest
		cacheConfig.add(
				createCacheConfig( "Hypothesis" )
						.appendField( "description", String.class )
						.appendField( "pos", Integer.class )
						.appendField( "date", Date.class )
						.appendField( "author_id", Long.class )
						.build()
		);
		cacheConfig.add(
				createCacheConfig( "Helicopter" )
						.appendField( "helicopterName", String.class )
						.appendField( "make", String.class )
						.build()
		);
		cacheConfig.add(
				createCacheConfig( "Author" )
						.withKeyType( Long.class )
						.appendIndex( "address_id", Long.class )
						.appendField( "name", String.class )
						.build()
		);
// QueriesWithEmbeddedTest
		cacheConfig.add(
				createCacheConfig( "StoryGame" )
						.withKeyType( Long.class )
						.appendIndex( "id", Long.class )
						.appendField( "storyText", String.class )
						.appendField( "evilText", String.class )
						.appendField( "goodText", String.class )
						.appendField( "text", String.class )
						.appendField( "score", Integer.class )
						.build()
		);
// QueryWithParametersTest
		cacheConfig.add(
				createCacheConfig( "Movie" )
						.appendIndex( "id", String.class )
						.appendField( "genre", Integer.class )
						.appendField( "title", String.class )
						.appendField( "suitableForKids", String.class )
						.appendField( "releaseDate", Date.class )
						.appendField( "viewerRating", Byte.class )
						.build()
		);

		config.setCacheConfiguration( cacheConfig.toArray( new CacheConfiguration[ cacheConfig.size() ] ) );

		return config;
	}

	private CacheConfiguration simpleCacheConfig( String name, Class<?> keyType ) {
		return new TestCacheConfigBuilder( name )
						.withKeyType( keyType )
						.build();
	}

	private CacheConfiguration simpleCacheConfig( String name ) {
		return new TestCacheConfigBuilder( name ).build();
	}

	private TestCacheConfigBuilder createCacheConfig( String name ) {
		return new TestCacheConfigBuilder( name );
	}

	private class TestCacheConfigBuilder {

		private CacheConfiguration cacheConfig;
		private QueryEntity queryEntity;
		private Map<String, QueryIndex> indexes;
		private Class<?> keyType = String.class;
		private String keyTypeName = null;
		private boolean forceQueryEntity = false;

		public TestCacheConfigBuilder(String name) {
			cacheConfig = new CacheConfiguration();
			cacheConfig.setAtomicityMode( CacheAtomicityMode.TRANSACTIONAL );
			cacheConfig.setCacheMode( CacheMode.PARTITIONED );
			cacheConfig.setWriteSynchronizationMode( CacheWriteSynchronizationMode.FULL_SYNC );
			cacheConfig.setStartSize( 10 );
			cacheConfig.setBackups( 0 );
			cacheConfig.setAffinity( new RendezvousAffinityFunction( false, 2 ) );
			cacheConfig.setCopyOnRead( false );
			cacheConfig.setName( name );

			queryEntity = new QueryEntity();
			queryEntity.setValueType( name );
			indexes = new HashMap<>();
		}

		public TestCacheConfigBuilder appendField( String fieldName, Class<?> fieldType ) {
			queryEntity.addQueryField( fieldName, fieldType.getName(), null );
			return this;
		}

		public TestCacheConfigBuilder appendIndex( String fieldName, Class<?> fieldType ) {
			queryEntity.addQueryField( fieldName, fieldType.getName(), null );
			indexes.put( fieldName, new QueryIndex( fieldName, QueryIndexType.SORTED ) );
			return this;
		}

		public TestCacheConfigBuilder withForceQueryEntity() {
			forceQueryEntity = true;
			return this;
		}

		public TestCacheConfigBuilder withKeyType(Class<?> keyClass) {
			this.keyType = keyClass;
			forceQueryEntity = true;
			return this;
		}

		public TestCacheConfigBuilder withKeyType(String keyType) {
			this.keyTypeName = keyType;
			forceQueryEntity = true;
			return this;
		}

		public CacheConfiguration<String, BinaryObject> build() {
			if ( forceQueryEntity || !indexes.isEmpty() || !queryEntity.getFields().isEmpty() ) {
				queryEntity.setKeyType( keyTypeName != null ? keyTypeName : keyType.getName() );
				queryEntity.setIndexes( indexes.values() );
				cacheConfig.setQueryEntities( Arrays.asList( queryEntity ) );
			}
			if ( keyTypeName == null ) {
				cacheConfig.setTypes( keyType, Object.class );
			}
			return cacheConfig;
		}
	}
}
