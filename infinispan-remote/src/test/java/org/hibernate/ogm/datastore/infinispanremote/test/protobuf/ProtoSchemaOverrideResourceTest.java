/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.protobuf;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteProperties;
import org.hibernate.ogm.datastore.infinispanremote.impl.InfinispanRemoteDatastoreProvider;
import org.hibernate.ogm.datastore.infinispanremote.utils.InfinispanRemoteTestHelper;
import org.hibernate.ogm.datastore.infinispanremote.utils.RemoteHotRodServerRule;
import org.hibernate.ogm.util.impl.ResourceHelper;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.service.spi.ServiceException;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

/**
 * Testing user defined proto schema
 *
 * @author Fabio Massimo Ercoli
 */
@TestForIssue(jiraKey = "OGM-1483")
public class ProtoSchemaOverrideResourceTest {

	@ClassRule
	public static final RemoteHotRodServerRule hotRodServer = new RemoteHotRodServerRule();

	private static final String PROTOSCHEMA_USERDEFINED_FOLDER = "protoschema-userdefined/";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void tryToRegisterResourceThatDoesNotExist() {
		try ( SessionFactory sf = TestHelper.getDefaultTestSessionFactory(
				Collections.singletonMap( InfinispanRemoteProperties.SCHEMA_OVERRIDE_RESOURCE, "file-does-not-exist.proto" ), SimpleEntity.class ) ) {
			fail( "Expected exception at Hibernate factory creation time was not raised" );
		}
		catch (ServiceException spiException) {
			Throwable cause = spiException.getCause();

			assertThat( cause ).isExactlyInstanceOf( HibernateException.class );
			assertThat( cause ).hasMessage( "OGM000055: Invalid URL given for configuration property 'hibernate.ogm.infinispan_remote.schema_override_resource':" +
					" file-does-not-exist.proto; The specified resource could not be found." );
		}
	}

	@Test
	public void tryToRegisterNotCompatibleProtoSchema() {
		try ( SessionFactory sf = TestHelper.getDefaultTestSessionFactory(
				Collections.singletonMap( InfinispanRemoteProperties.SCHEMA_OVERRIDE_RESOURCE, PROTOSCHEMA_USERDEFINED_FOLDER + "entity-wrong-schema.proto" ),
				SimpleEntity.class
		) ) {
			fail( "Expected exception at Hibernate factory creation time was not raised" );
		}
		catch (HibernateException hibException) {
			assertThat( hibException.getMessage() ).startsWith( "OGM001725" );
		}
	}

	@Test
	public void registerUserDefinedProtoSchemaUsingEntity() {
		registerUserDefinedProtoSchema( PROTOSCHEMA_USERDEFINED_FOLDER + "entity-schema.proto", SimpleEntity.class );
	}

	@Test
	public void registerUserDefinedProtoSchemaUsingSequenceGeneratorEntity() {
		registerUserDefinedProtoSchema( PROTOSCHEMA_USERDEFINED_FOLDER + "sequence-gen-schema.proto", EntityWithSequenceGenerator.class );
	}

	@Test
	public void registerUserDefinedProtoSchemaUsingTableGeneratorEntity() {
		registerUserDefinedProtoSchema( PROTOSCHEMA_USERDEFINED_FOLDER + "table-gen-schema.proto", EntityWithTableGenerator.class );
	}

	@Test
	public void registerUserDefinedProtoSchemaUsingEntitiesAndIdGenerators() {
		registerUserDefinedProtoSchema(
				PROTOSCHEMA_USERDEFINED_FOLDER + "entities-id-generators-schema.proto",
				SimpleEntity.class, EntityWithTableGenerator.class, EntityWithSequenceGenerator.class
		);
	}

	private void registerUserDefinedProtoSchema(String resource, Class<?>... entities) {
		try ( SessionFactory sessionFactory = TestHelper.getDefaultTestSessionFactory(
				Collections.singletonMap( InfinispanRemoteProperties.SCHEMA_OVERRIDE_RESOURCE, resource ),
				entities
		) ) {
			InfinispanRemoteDatastoreProvider provider = InfinispanRemoteTestHelper.getProvider( sessionFactory );

			URL schemaOverrideResource = provider.getSchemaOverrideResource();
			assertThat( schemaOverrideResource.getFile() ).endsWith( resource );

			String expectedProtoSchema = null;
			try {
				expectedProtoSchema = ResourceHelper.readResource( schemaOverrideResource );
			}
			catch (IOException ex) {
				throw new RuntimeException( "Unexpected error loading resource " + schemaOverrideResource );
			}

			RemoteCache<Object, Object> cache = provider.getCache( ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME );
			String protoSchema = (String) cache.get( provider.getSchemaFileName() );
			assertThat( protoSchema ).isEqualTo( expectedProtoSchema );
		}
	}
}
