/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteDialect;
import org.hibernate.ogm.datastore.infinispanremote.configuration.impl.InfinispanRemoteConfiguration;
import org.hibernate.ogm.datastore.infinispanremote.impl.protobuf.SchemaDefinitions;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.OgmProtoStreamMarshaller;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtoDataMapper;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamSerializerSetup;
import org.hibernate.ogm.datastore.infinispanremote.impl.schema.SequenceTableDefinition;
import org.hibernate.ogm.datastore.infinispanremote.impl.sequences.HotRodSequenceHandler;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.infinispanremote.schema.spi.SchemaCapture;
import org.hibernate.ogm.datastore.infinispanremote.schema.spi.SchemaOverride;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.util.impl.EffectivelyFinal;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

/**
 * @author Sanne Grinovero
 */
public class InfinispanRemoteDatastoreProvider extends BaseDatastoreProvider
				implements Startable, Stoppable, Configurable, ServiceRegistryAwareService {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	/**
	 * The org.infinispan.commons.marshall.Marshaller instance which shall be used
	 * by our Hot Rod client.
	 */
	private final OgmProtoStreamMarshaller marshaller = new OgmProtoStreamMarshaller();

	// Only available during configuration
	private InfinispanRemoteConfiguration config;

	private boolean createCachesEnabled = false;

	// The Hot Rod client; maintains TCP connections to the datagrid.
	@EffectivelyFinal
	private RemoteCacheManager hotrodClient;

	//Useful to allow people to dump the generated schema,
	//we use it to capture the schema in tests too.
	@EffectivelyFinal
	private SchemaCapture schemaCapture;

	@EffectivelyFinal
	private ServiceRegistryImplementor serviceRegistry;

	@EffectivelyFinal
	private SchemaOverride schemaOverrideService;

	@EffectivelyFinal
	private Map<String, String> cacheTemplatesByName;

	//For each cache we have a schema and a set of encoders/decoders to the generated protobuf schema
	@EffectivelyFinal
	private Map<String,ProtoDataMapper> perCacheSchemaMappers;

	@EffectivelyFinal
	private HotRodSequenceHandler sequences;

	@EffectivelyFinal
	private SchemaDefinitions sd;

	@EffectivelyFinal
	private String schemaPackageName;

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return InfinispanRemoteDialect.class;
	}

	@Override
	public void start() {
		hotrodClient = HotRodClientBuilder.builder().withConfiguration( config, marshaller ).build();
		hotrodClient.start();
		config = null; //no longer needed
	}

	public RemoteCacheManager getRemoteCacheManager() {
		return hotrodClient;
	}

	@Override
	public void stop() {
		hotrodClient.stop();
	}

	@Override
	public void configure(Map configurationValues) {
		this.config = new InfinispanRemoteConfiguration();
		this.config.initConfiguration( configurationValues, serviceRegistry );
		this.schemaCapture = config.getSchemaCaptureService();
		this.schemaOverrideService = config.getSchemaOverrideService();
		this.schemaPackageName = config.getSchemaPackageName();
		this.createCachesEnabled = config.isCreateCachesEnabled();
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		return ProtobufSchemaInitializer.class;
	}

	public void registerSchemaDefinitions(SchemaDefinitions sd) {
		this.sd = sd;
		sd.validateSchema();
		RemoteCache<String,String> protobufCache = getProtobufCache();
		//FIXME make this name configurable & give it a sensible default:
		final String generatedProtobufName = "Hibernate_OGM_Generated_schema.proto";
		sd.deploySchema( generatedProtobufName, protobufCache, schemaCapture, schemaOverrideService );
		this.sequences = new HotRodSequenceHandler( this, marshaller, sd.getSequenceDefinitions() );
		setCacheTemplatesByName( sd );
		startAndValidateCaches();
		perCacheSchemaMappers = sd.generateSchemaMappingAdapters( this, sd, marshaller );
	}

	private void startAndValidateCaches() {
		Set<String> failedCacheNames = new TreeSet<String>();

		cacheTemplatesByName.entrySet().forEach( entry -> {
			String cacheName = entry.getKey();
			String cacheTemplate = entry.getValue();
			RemoteCache<?, ?> cache = hotrodClient.getCache( cacheName );
			if ( cache == null && createCachesEnabled ) {
				hotrodClient.administration().createCache( cacheName, cacheTemplate );
				cache = hotrodClient.getCache( cacheName );
			}
			if ( cache == null ) {
				failedCacheNames.add( cacheName );
			}
		} );
		if ( failedCacheNames.size() > 1 ) {
			throw log.expectedCachesNotDefined( failedCacheNames );
		}
		else if ( failedCacheNames.size() == 1 ) {
			throw log.expectedCacheNotDefined( failedCacheNames.iterator().next() );
		}
	}

	private void setCacheTemplatesByName(SchemaDefinitions sd) {
		this.cacheTemplatesByName = sd.getCacheTemplateByName();
	}

	private RemoteCache<String, String> getProtobufCache() {
		return getCache( ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME );
	}

	@Override
	public boolean allowsTransactionEmulation() {
		// Hot Rod doesn't support "true" transaction yet
		return true;
	}

	public String getProtobufPackageName() {
		return schemaPackageName;
	}

	public Map<String, String> getCacheTemplatesByName() {
		return cacheTemplatesByName;
	}

	public ProtoStreamMappingAdapter getDataMapperForCache(String cacheName) {
		return perCacheSchemaMappers.get( cacheName );
	}

	public ProtostreamAssociationMappingAdapter getCollectionsDataMapper(String cacheName) {
		return perCacheSchemaMappers.get( cacheName );
	}

	public HotRodSequenceHandler getSequenceHandler() {
		return this.sequences;
	}

	public SerializationContext getSerializationContextForSequences(SequenceTableDefinition std) {
		//This method is here so that we can cache / reuse these contexts ?
		return ProtostreamSerializerSetup.buildSerializationContextForSequences( sd, std );
	}

	public <K, V> RemoteCache<K, V> getCache(String cacheName) {
		RemoteCache<K,V> cache = hotrodClient.getCache( cacheName );
		if ( cache == null ) {
			throw log.expectedCacheNotDefined( cacheName );
		}
		return cache;
	}

}
