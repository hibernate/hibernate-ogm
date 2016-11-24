/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.cache.QueryIndexType;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgnitionEx;
import org.apache.ignite.internal.binary.BinaryMarshaller;
import org.apache.ignite.logger.slf4j.Slf4jLogger;
import org.hibernate.ogm.datastore.ignite.IgniteConfigurationBuilder;

/**
 * Ignite cache configuration for tests
 *
 * @author Victor Kadachigov
 */
public class IgniteTestConfigurationBuilder implements IgniteConfigurationBuilder {

	@Override
	public IgniteConfiguration build() {

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

		config.setGridName( "OgmTestGrid" );
		config.setLocalHost( "127.0.0.1" );
		config.setPeerClassLoadingEnabled( true );
		config.setClientMode( false );
		config.setMarshaller( new BinaryMarshaller() );
		config.setGridLogger( new Slf4jLogger() );
		config.setPublicThreadPoolSize( 2 );

		List<CacheConfiguration> cacheConfig = new ArrayList<>();

// EmbeddableIdTest
		cacheConfig.add( createCacheConfig( "SingleBoardComputer" ).withForceQueryEntity().build() );
// SequenceIdGeneratorTest
		cacheConfig.add( simpleCacheConfig( "Song", Long.class ) );
		cacheConfig.add( simpleCacheConfig( "Actor", Long.class ) );
// JPAResourceLocalTest
		cacheConfig.add( simpleCacheConfig( "Poem" ) );
// ManyToManyTest
		cacheConfig.add( simpleCacheConfig( "Car" ) );
		cacheConfig.add( simpleCacheConfig( "Tire" ) );
		cacheConfig.add( simpleCacheConfig( "Car_Tire" ) );
		cacheConfig.add( simpleCacheConfig( "AccountOwner" ) );
		cacheConfig.add( simpleCacheConfig( "BankAccount" ) );
		cacheConfig.add( simpleCacheConfig( "AccountOwner_BankAccount" ) );
// ListTest
		cacheConfig.add( createCacheConfig( "Child" ).appendIndex( "Father_id", String.class ).build() );
		cacheConfig.add( simpleCacheConfig( "Father" ) );
		cacheConfig.add( createCacheConfig( "Father_child" ).appendIndex( "Father_id", String.class ).build() );
		cacheConfig.add( simpleCacheConfig( "GrandChild" ) );
		cacheConfig.add( createCacheConfig( "GrandMother" ).appendIndex( "id", String.class ).build() );
		cacheConfig.add( createCacheConfig( "GrandMother_grandChildren" ).appendIndex( "GrandMother_id", String.class ).build() );
		cacheConfig.add( simpleCacheConfig( "Race" ) );
		cacheConfig.add( simpleCacheConfig( "Runner" ) );
		cacheConfig.add( createCacheConfig( "Race_Runners" ).appendIndex( "Race_raceId", String.class ).build() );
// ManyToManyExtraTest
		cacheConfig.add( simpleCacheConfig( "Student" ) );
		cacheConfig.add( simpleCacheConfig( "ClassRoom", Long.class ) );
		cacheConfig.add( simpleCacheConfig( "ClassRoom_Student" ) );
// MapTest
		cacheConfig.add( simpleCacheConfig( "PhoneNumber" ) );
		cacheConfig.add( simpleCacheConfig( "Enterprise" ) );
		cacheConfig.add( createCacheConfig( "Enterprise_revenueByDepartment" ).appendIndex( "Enterprise_id", String.class ).build() );
		cacheConfig.add( createCacheConfig( "Enterprise_departments" ).appendIndex( "Enterprise_id", String.class ).build() );
		cacheConfig.add( simpleCacheConfig( "Department" ) );
		cacheConfig.add( simpleCacheConfig( "User" ) );
		cacheConfig.add(
				createCacheConfig( "Address" )
						.appendIndex( "id", String.class )
						.appendField( "street", String.class )
						.appendField( "city", String.class )
						.build()
		);
		cacheConfig.add( createCacheConfig( "User_Address" ).appendIndex( "User_id", String.class ).build() );
		cacheConfig.add( createCacheConfig( "User_PhoneNumber" ).appendIndex( "User_id", String.class ).build() );
		cacheConfig.add( createCacheConfig( "Nicks" ).appendIndex( "user_id", String.class ).build() );
// CollectionUnidirectionalTest
		cacheConfig.add( simpleCacheConfig( "Cloud" ) );
		cacheConfig.add( simpleCacheConfig( "SnowFlake" ) );
		cacheConfig.add( createCacheConfig( "joinProducedSnowflakes" ).appendIndex( "Cloud_id", String.class ).build() );
		cacheConfig.add( simpleCacheConfig( "joinBackupSnowflakes" ) );
// ReferencedCompositeIdTest
		cacheConfig.add(
				createCacheConfig( "Director" )
						.build()
		);
		cacheConfig.add(
				createCacheConfig( "Tournament" )
						.withKeyType( "TournamentId" )
						.build()
		);
		cacheConfig.add(
				createCacheConfig( "Director_Tournament" )
						.withForceQueryEntity()
						.appendIndex( "Director_id", String.class )
						.build()
		);
// ManyToOneExtraTest
		cacheConfig.add( simpleCacheConfig( "Basket" ) );
		cacheConfig.add( simpleCacheConfig( "Product" ) );
		cacheConfig.add( createCacheConfig( "Basket_Product" ).appendIndex( "Basket_id", String.class ).build() );
// ManyToOneTest
		cacheConfig.add( simpleCacheConfig( "JUG" ) );
		cacheConfig.add( simpleCacheConfig( "Member" ) );
		cacheConfig.add( simpleCacheConfig( "SalesForce" ) );
		cacheConfig.add( createCacheConfig( "SalesGuy" ).appendIndex( "salesForce_id", String.class ).build() );
		cacheConfig.add( createCacheConfig( "Beer" ).appendIndex( "brewery_id", String.class ).build() );
		cacheConfig.add( simpleCacheConfig( "Brewery" ) );
		cacheConfig.add(
				createCacheConfig( "Game" )
						.withKeyType( "GameId" )
						.appendIndex( "playedOn_id_countryCode", String.class )
						.appendIndex( "playedOn_id_sequenceNo", Integer.class )
						.build()
		);
		cacheConfig.add(
				createCacheConfig( "Court" )
					.withKeyType( "CourtId" )
					.build()
		);
// OneToOneTest
		cacheConfig.add( simpleCacheConfig( "Horse" ) );
		cacheConfig.add( simpleCacheConfig( "Cavalier" ) );
		cacheConfig.add( simpleCacheConfig( "Vehicule" ) );
		cacheConfig.add( simpleCacheConfig( "Wheel" ) );
		cacheConfig.add( createCacheConfig( "Husband" ).appendIndex( "wife", String.class ).build() );
		cacheConfig.add( simpleCacheConfig( "Wife" ) );
// CompositeIdTest
		cacheConfig.add(
				createCacheConfig( "News" )
						.withKeyType( "NewsID" )
						.appendField( "content", String.class )
						.build()
		);
		cacheConfig.add(
				createCacheConfig( "Label" )
						.withKeyType( Long.class )
						.appendIndex( "news_author_fk", String.class )
						.appendIndex( "news_topic_fk", String.class )
						.build()
		);
		cacheConfig.add( simpleCacheConfig( "News_Label" ) );
// AutoIdGeneratorWithSessionTest
		cacheConfig.add( simpleCacheConfig( "DistributedRevisionControl", Long.class ) );
// TableIdGeneratorTest
		cacheConfig.add( simpleCacheConfig( "Music", Long.class ) );
		cacheConfig.add( simpleCacheConfig( "Video", Integer.class ) );
		cacheConfig.add( simpleCacheConfig( "Composer", Long.class ) );
// EmbeddableExtraTest
		cacheConfig.add( simpleCacheConfig( "MultiAddressAccount" ) );
		cacheConfig.add( simpleCacheConfig( "AccountWithPhone" ) );
		cacheConfig.add( simpleCacheConfig( "Order" ) );
		cacheConfig.add( simpleCacheConfig( "AccountWithPhone_phoneNumber" ) );
		cacheConfig.add( simpleCacheConfig( "MultiAddressAccount_addresses" ) );
		cacheConfig.add( simpleCacheConfig( "Order_shippingAddress" ) );
// SharedPrimaryKeyTest
		cacheConfig.add( simpleCacheConfig( "CoffeeMug" ) );
		cacheConfig.add( simpleCacheConfig( "Lid" ) );
// JPAPolymorphicCollectionTest
		cacheConfig.add( simpleCacheConfig( "Hero" ) );
		cacheConfig.add( simpleCacheConfig( "SuperHero" ) );
		cacheConfig.add( simpleCacheConfig( "HeroClub" ) );
		cacheConfig.add( simpleCacheConfig( "HeroClub_Hero" ) );
// JPATablePerClassFindTest
		cacheConfig.add( simpleCacheConfig( "CommunityMember" ) );
		cacheConfig.add( createCacheConfig( "Employee" ).appendIndex( "EmployerID", String.class ).build() );
// InnerClassFindTest
		cacheConfig.add( simpleCacheConfig( "employee" ) );
// SimpleQueriesTest
		cacheConfig.add(
				createCacheConfig( "Hypothesis" )
						.appendIndex( "id", String.class )
						.appendField( "description", String.class )
						.appendField( "pos", Integer.class )
						.appendField( "date", Date.class )
						.appendField( "author_id", Long.class )
						.build()
		);
		cacheConfig.add(
				createCacheConfig( "Helicopter" )
						.appendIndex( "uuid", String.class )
						.appendField( "helicopterName", String.class )
						.appendField( "make", String.class )
						.build()
		);
		cacheConfig.add(
				createCacheConfig( "Author" )
						.withKeyType( Long.class )
						.appendIndex( "id", Long.class )
						.appendIndex( "address_id", Long.class )
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
// BuiltInTypeTest
		cacheConfig.add(
				createCacheConfig( "Bookmark" )
						.appendIndex( "id", String.class )
						.appendField( "isPrivate", String.class )
						.appendField( "isRead", String.class )
						.appendField( "isShared", Integer.class )
						.appendField( "classifier", String.class )
						.appendField( "classifierAsOrdinal", Byte.class )
						.appendField( "creationDate", Date.class )
						.appendField( "destructionDate", Date.class )
						.appendField( "creationCalendar", Date.class )
						.appendField( "destructionCalendar", Date.class )
						.appendField( "siteWeight", BigDecimal.class )
						.appendField( "visitCount", BigInteger.class )
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

		private CacheConfiguration<String, BinaryObject> cacheConfig;
		private QueryEntity queryEntity;
		private Map<String, QueryIndex> indexes;
		private String keyType = String.class.getName();
		private boolean forceQueryEntity = false;

		public TestCacheConfigBuilder(String name) {
			cacheConfig = new CacheConfiguration<>();
			cacheConfig.setAtomicityMode( CacheAtomicityMode.TRANSACTIONAL );
			cacheConfig.setCacheMode( CacheMode.PARTITIONED );
			cacheConfig.setStartSize( 10 );
			cacheConfig.setBackups( 0 );
			cacheConfig.setAffinity( new RendezvousAffinityFunction( false, 2 ) );
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
			this.keyType = keyClass.getName();
			forceQueryEntity = true;
			return this;
		}

		public TestCacheConfigBuilder withKeyType(String keyType) {
			this.keyType = keyType;
			forceQueryEntity = true;
			return this;
		}

		public CacheConfiguration<String, BinaryObject> build() {
			if ( forceQueryEntity || !indexes.isEmpty() || !queryEntity.getFields().isEmpty() ) {
				queryEntity.setKeyType( keyType );
				queryEntity.setIndexes( indexes.values() );
				cacheConfig.setQueryEntities( Arrays.asList( queryEntity ) );
			}
			return cacheConfig;
		}
	}
}
