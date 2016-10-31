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
import org.hibernate.ogm.datastore.ignite.IgniteConfigurationBuilder;

/**
 * Ignite cache configuration for tests
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
		cacheConfig.add( createCacheConfig( "PhoneNumber" ) );
		cacheConfig.add( createCacheConfig( "Enterprise" ) );
		cacheConfig.add( createCacheConfig( "Enterprise_revenueByDepartment", "Enterprise_revenueByDepartment" ) );
		cacheConfig.add( createCacheConfig( "Enterprise_departments", "Enterprise_revenueByDepartment" ) );
		cacheConfig.add( createCacheConfig( "Department" ) );
		cacheConfig.add( createCacheConfig( "User" ) );
		cacheConfig.add(
			addQueryField( addQueryField(
				createCacheConfig( "Address", "id" ),
					"street", String.class ),
					"city", String.class)
		);
		cacheConfig.add( createCacheConfig( "User_Address", "User_id" ) );
		cacheConfig.add( createCacheConfig( "User_PhoneNumber", "User_id" ) );
		cacheConfig.add( createCacheConfig( "Nicks", "user_id" ) );
// CollectionUnidirectionalTest
		cacheConfig.add( createCacheConfig( "Cloud" ) );
		cacheConfig.add( createCacheConfig( "SnowFlake" ) );
		cacheConfig.add( createCacheConfig( "joinProducedSnowflakes", "Cloud_id" ) );
		cacheConfig.add( createCacheConfig( "joinBackupSnowflakes" ) );
// ReferencedCompositeIdTest
		cacheConfig.add( createCacheConfig( "Director" ) );
		cacheConfig.add( createCacheConfig( "Tournament" ) );
		cacheConfig.add( createCacheConfig( "Director_Tournament" ) );
// ManyToOneExtraTest
		cacheConfig.add( createCacheConfig( "Basket" ) );
		cacheConfig.add( createCacheConfig( "Product" ) );
		cacheConfig.add( createCacheConfig( "Basket_Product", "Basket_id" ) );
// ManyToOneTest	
		cacheConfig.add( createCacheConfig( "JUG" ) );
		cacheConfig.add( createCacheConfig( "Member" ) );
		cacheConfig.add( createCacheConfig( "SalesForce" ) );
		cacheConfig.add( createCacheConfig( "SalesGuy", "salesForce_id" ) );
		cacheConfig.add( createCacheConfig( "Beer", "brewery_id" ) );
		cacheConfig.add( createCacheConfig( "Brewery" ) );
		cacheConfig.add( createCacheConfig( "Game" ) );
		cacheConfig.add( createCacheConfig( "Court" ) );
// OneToOneTest
		cacheConfig.add( createCacheConfig( "Horse" ) );
		cacheConfig.add( createCacheConfig( "Cavalier" ) );
		cacheConfig.add( createCacheConfig( "Vehicule" ) );
		cacheConfig.add( createCacheConfig( "Wheel" ) );
		cacheConfig.add( createCacheConfig( "Husband", "wife" ) );
		cacheConfig.add( createCacheConfig( "Wife" ) );
// CompositeIdTest
		cacheConfig.add(
			addQueryField(
				createCacheConfig( "News", "id" ),
				"content", String.class
			)
		);
		cacheConfig.add( createCacheConfig( "Label" ) );
		cacheConfig.add( createCacheConfig( "News_Label" ) );
// AutoIdGeneratorWithSessionTest
		cacheConfig.add( createCacheConfig( "DistributedRevisionControl" ) );
// TableIdGeneratorTest
		cacheConfig.add( createCacheConfig( "Music" ) );
		cacheConfig.add( createCacheConfig( "Video" ) );
		cacheConfig.add( createCacheConfig( "Composer" ) );
// EmbeddableExtraTest
		cacheConfig.add( createCacheConfig( "MultiAddressAccount" ) );
		cacheConfig.add( createCacheConfig( "AccountWithPhone" ) );
		cacheConfig.add( createCacheConfig( "Order" ) );
		cacheConfig.add( createCacheConfig( "AccountWithPhone_phoneNumber" ) );
		cacheConfig.add( createCacheConfig( "MultiAddressAccount_addresses" ) );
		cacheConfig.add( createCacheConfig( "Order_shippingAddress" ) );
// SharedPrimaryKeyTest
		cacheConfig.add( createCacheConfig( "CoffeeMug" ) );
		cacheConfig.add( createCacheConfig( "Lid" ) );
// JPAPolymorphicCollectionTest
		cacheConfig.add( createCacheConfig( "Hero" ) );
		cacheConfig.add( createCacheConfig( "SuperHero" ) );
		cacheConfig.add( createCacheConfig( "HeroClub" ) );
		cacheConfig.add( createCacheConfig( "HeroClub_Hero" ) );
// JPATablePerClassFindTest
		cacheConfig.add( createCacheConfig( "CommunityMember" ) );
		cacheConfig.add( createCacheConfig( "Employee", "EmployerID" ) );
// InnerClassFindTest
		cacheConfig.add( createCacheConfig( "employee" ) );
// SimpleQueriesTest
		cacheConfig.add( 
			addQueryField( addQueryField( addQueryField( addQueryField(	
				createCacheConfig( "Hypothesis", "id" ),
					"description", String.class ),
					"pos", Integer.class ),
					"date", Date.class ),
					"author_id", Long.class )
		);
		cacheConfig.add( 
			addQueryField( addQueryField( 
				createCacheConfig( "Helicopter", "uuid" ),
					"helicopterName", String.class ),
					"make", String.class )
		);
		cacheConfig.add( 
			addQueryField(
				createCacheConfig( "Author", "id" ),
					"address_id", String.class )
		);
// QueriesWithEmbeddedTest
		cacheConfig.add( 
			addQueryField( addQueryField( addQueryField( addQueryField(	addQueryField(
				createCacheConfig( "StoryGame", "id" ),
					"storyText", String.class ),
					"evilText", String.class ),
					"goodText", String.class ),
					"text", String.class ),
					"score", Integer.class )
		);
// QueryWithParametersTest
		cacheConfig.add( 
			addQueryField( addQueryField( addQueryField( addQueryField(	addQueryField(
				createCacheConfig( "Movie", "id" ),
					"genre", Integer.class ),
					"title", String.class ),
					"suitableForKids", String.class ),
					"releaseDate", Date.class ),
					"viewerRating", Byte.class )
		);
// BuiltInTypeTest
		cacheConfig.add( 
			addQueryField( addQueryField( addQueryField( addQueryField( addQueryField( addQueryField( addQueryField( addQueryField( addQueryField( addQueryField( addQueryField(
				createCacheConfig( "Movie", "id" ),
					"isPrivate", String.class ),
					"isRead", String.class ),
					"isShared", Integer.class ),
					"classifier", String.class ),
					"classifierAsOrdinal", Byte.class ),
					"creationDate", Date.class ),
					"destructionDate", Date.class ),
					"creationCalendar", Date.class ),
					"destructionCalendar", Date.class ),
					"siteWeight", BigDecimal.class ),
					"visitCount", BigInteger.class )
		);
		
		
		
		config.setCacheConfiguration(cacheConfig.toArray( new CacheConfiguration[cacheConfig.size()] ));
		
		return config;
	}
	
	private CacheConfiguration addQueryField( CacheConfiguration cacheConfig, String fullName, Class<?> type ) {
		QueryEntity queryEntity = (QueryEntity)cacheConfig.getQueryEntities().iterator();
		queryEntity.addQueryField(fullName, type.getName(), null);
		return cacheConfig;
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
