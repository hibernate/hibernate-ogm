/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteDialect;
import org.hibernate.ogm.datastore.infinispanremote.configuration.impl.InfinispanRemoteConfiguration;
import org.hibernate.ogm.datastore.infinispanremote.impl.cachehandler.HotRodCacheCreationHandler;
import org.hibernate.ogm.datastore.infinispanremote.impl.cachehandler.HotRodCacheHandler;
import org.hibernate.ogm.datastore.infinispanremote.impl.cachehandler.HotRodCacheValidationHandler;
import org.hibernate.ogm.datastore.infinispanremote.impl.protobuf.schema.SchemaDefinitions;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.OgmProtoStreamMarshaller;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtoDataMapper;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamSerializerSetup;
import org.hibernate.ogm.datastore.infinispanremote.impl.schema.SequenceTableDefinition;
import org.hibernate.ogm.datastore.infinispanremote.impl.sequences.HotRodSequenceHandler;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.InfinispanRemoteBasedQueryParserService;
import org.hibernate.ogm.datastore.infinispanremote.schema.spi.SchemaCapture;
import org.hibernate.ogm.datastore.infinispanremote.schema.spi.SchemaOverride;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.ogm.util.impl.EffectivelyFinal;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

/**
 * @author Sanne Grinovero
 * @author Fabio Massimo Ercoli
 */
public class InfinispanRemoteDatastoreProvider extends BaseDatastoreProvider
				implements Startable, Stoppable, Configurable, ServiceRegistryAwareService {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private static final String SCRIPT_CACHE_NAME = "___script_cache";

	/**
	 * The org.infinispan.commons.marshall.Marshaller instance which shall be used
	 * by our Hot Rod client.
	 */
	private final OgmProtoStreamMarshaller marshaller = new OgmProtoStreamMarshaller();

	// Only available during configuration
	private InfinispanRemoteConfiguration config;

	private boolean createCachesEnabled = false;

	@EffectivelyFinal
	private String cacheConfiguration;

	// The Hot Rod client; maintains TCP connections to the datagrid.
	@EffectivelyFinal
	private RemoteCacheManager hotrodClient;

	@EffectivelyFinal
	private RemoteCacheManager scriptManager;

	//Useful to allow people to dump the generated schema,
	//we use it to capture the schema in tests too.
	@EffectivelyFinal
	private SchemaCapture schemaCapture;

	@EffectivelyFinal
	private ServiceRegistryImplementor serviceRegistry;

	@EffectivelyFinal
	private SchemaOverride schemaOverrideService;

	@EffectivelyFinal
	private URL schemaOverrideResource;

	//For each cache we have a schema and a set of encoders/decoders to the generated protobuf schema
	@EffectivelyFinal
	private Map<String,ProtoDataMapper> perCacheSchemaMappers;

	@EffectivelyFinal
	private HotRodSequenceHandler sequences;

	@EffectivelyFinal
	private HotRodCacheHandler cacheHandler;

	@EffectivelyFinal
	private SchemaDefinitions sd;

	@EffectivelyFinal
	private String schemaPackageName;

	@EffectivelyFinal
	private String schemaFileName;

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return InfinispanRemoteDialect.class;
	}

	@Override
	public void start() {
		hotrodClient = HotRodClientBuilder.builder().withConfiguration( config, marshaller ).build();
		// TODO temporary create another cache manager to handle remote script registration and invocation due to incompatibility of ProtoStreamMarshaller.
		// When https://issues.jboss.org/browse/ISPN-8020 is closed, we could remove it and reuse the common hotrodClient.
		scriptManager = HotRodClientBuilder.builder().withConfiguration( config, new GenericJBossMarshaller() ).build();
		config = null; //no longer needed
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
		this.schemaOverrideResource = config.getSchemaOverrideResource();
		this.schemaPackageName = config.getSchemaPackageName();
		this.schemaFileName = config.getSchemaFileName();
		this.createCachesEnabled = config.isCreateCachesEnabled();
		this.cacheConfiguration = config.getCacheConfiguration();
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
		this.sd.validateSchema();
		RemoteCache<String, String> protobufCache = getProtobufCache();
		this.sd.deploySchema( schemaFileName, protobufCache, schemaCapture, schemaOverrideService, schemaOverrideResource );

		// register proto schema also to global serialization context used for unmarshalling
		registerProtoFiles( marshaller, sd );

		this.sequences = new HotRodSequenceHandler( this, marshaller, sd.getSequenceDefinitions() );
		this.cacheHandler = createCacheHandler( sd );

		startCaches( cacheHandler, hotrodClient );

		this.perCacheSchemaMappers = sd.generateSchemaMappingAdapters( this, sd, marshaller );
	}

	private void registerProtoFiles(OgmProtoStreamMarshaller marshaller, SchemaDefinitions sd) {
		try {
			marshaller.getSerializationContext().registerProtoFiles( sd.asFileDescriptorSource() );
		}
		catch (DescriptorParserException | IOException e) {
			throw log.errorAtProtobufParsing( e );
		}
	}

	private void startCaches(HotRodCacheHandler cacheHandler, RemoteCacheManager hotrodClient) {
		try {
			cacheHandler.startAndValidateCaches( hotrodClient );
		}
		catch (HotRodClientException ex) {
			throw log.errorAtCachesStart( ex );
		}
	}

	private HotRodCacheHandler createCacheHandler(SchemaDefinitions sd) {
		if ( createCachesEnabled ) {
			return new HotRodCacheCreationHandler( cacheConfiguration, sd.getCacheConfigurationByName() );
		}
		else {
			return new HotRodCacheValidationHandler( sd.getCacheConfigurationByName().keySet() );
		}
	}

	private RemoteCache<String, String> getProtobufCache() {
		return getCache( ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME );
	}

	@Override
	public boolean allowsTransactionEmulation() {
		// Hot Rod doesn't support "true" transaction yet
		return true;
	}

	@Override
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		return InfinispanRemoteBasedQueryParserService.class;
	}

	public URL getSchemaOverrideResource() {
		return schemaOverrideResource;
	}

	public String getProtobufPackageName() {
		return schemaPackageName;
	}

	public String getSchemaFileName() {
		return schemaFileName;
	}

	public String getConfiguration(String cacheName) {
		return cacheHandler.getConfiguration( cacheName );
	}

	public Set<String> getMappedCacheNames() {
		return cacheHandler.getCaches();
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
			throw log.expectedCachesNotDefined( Collections.singleton( cacheName ) );
		}
		return cache;
	}

	public <K, V> RemoteCache<K, V> getScriptCache() {
		return scriptManager.getCache( SCRIPT_CACHE_NAME );
	}

	public <K, V> RemoteCache<K, V> getScriptExecutorCache() {
		return scriptManager.getCache();
	}
}
