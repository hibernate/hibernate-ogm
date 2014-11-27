/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.externalizer.impl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;

import org.hibernate.ogm.datastore.infinispan.InfinispanDialect;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.common.externalizer.impl.ExternalizerIds;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.common.externalizer.impl.VersionChecker;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.infinispan.commons.marshall.AdvancedExternalizer;

/**
 * An externalizer for serializing and de-serializing {@link EntityKeyMetadata} instances. Implicitly used by
 * {@link InfinispanDialect} during mass-indexing where {@code EntityKeyMetadata} instances are sent to the cluster
 * nodes in order to find the matching entries from the global entity cache.
 * <p>
 * This externalizer is automatically registered with the cache manager when starting the
 * {@link InfinispanDatastoreProvider}, so it's not required to configure the externalizer in the Infinispan
 * configuration file.
 *
 * @author Gunnar Morling
 */
// As an implementation of AdvancedExternalizer this is never serialized according to the Externalizer docs
@SuppressWarnings("serial")
public class EntityKeyMetadataExternalizer implements AdvancedExternalizer<EntityKeyMetadata> {

	public static final EntityKeyMetadataExternalizer INSTANCE = new EntityKeyMetadataExternalizer();

	/**
	 * Format version of the key type; allows to apply version dependent deserialization logic in the future if
	 * required; to be incremented when adding new fields to the serialized structure
	 */
	private static final int VERSION = 1;

	private static final Set<Class<? extends EntityKeyMetadata>> TYPE_CLASSES = Collections
			.<Class<? extends EntityKeyMetadata>>singleton( DefaultEntityKeyMetadata.class );

	private EntityKeyMetadataExternalizer() {
	}

	@Override
	public void writeObject(ObjectOutput output, EntityKeyMetadata metadata) throws IOException {
		output.writeInt( VERSION );
		output.writeUTF( metadata.getTable() );
		output.writeObject( metadata.getColumnNames() );
	}

	@Override
	public EntityKeyMetadata readObject(ObjectInput input) throws IOException, ClassNotFoundException {
		VersionChecker.readAndCheckVersion( input, VERSION, EntityKeyMetadata.class );

		String tableName = input.readUTF();
		String[] columnNames = (String[]) input.readObject();

		return new DefaultEntityKeyMetadata( tableName, columnNames );
	}

	@Override
	public Set<Class<? extends EntityKeyMetadata>> getTypeClasses() {
		return TYPE_CLASSES;
	}

	@Override
	public Integer getId() {
		return ExternalizerIds.PER_KIND_ENTITY_METADATA;
	}
}
