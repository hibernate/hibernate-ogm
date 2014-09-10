/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.dialect.impl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;

import org.hibernate.ogm.datastore.infinispan.InfinispanDialect;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.infinispan.commons.marshall.AdvancedExternalizer;

/**
 * An externalizer for serializing and de-serializing {@link EntityKey} instances. Implicitly used by
 * {@link InfinispanDialect} which stores keys as is in the Infinispan data store.
 * <p>
 * This externalizer is automatically registered with the cache manager when starting the
 * {@link InfinispanDatastoreProvider}, so it's not required to configure the externalizer in the Infinispan
 * configuration file.
 *
 * @author Gunnar Morling
 */
// As an implementation of AdvancedExternalizer this is never serialized according to the Externalizer docs
@SuppressWarnings("serial")
public class EntityKeyExternalizer implements AdvancedExternalizer<EntityKey> {

	public static final EntityKeyExternalizer INSTANCE = new EntityKeyExternalizer();

	/**
	 * Format version of the key type; allows to apply version dependent deserialization logic in the future if
	 * required; to be incremented when adding new fields to the serialized structure
	 */
	private static final int VERSION = 1;

	private static final Set<Class<? extends EntityKey>> TYPE_CLASSES = Collections.<Class<? extends EntityKey>>singleton( EntityKey.class );

	private EntityKeyExternalizer() {
	}

	@Override
	public void writeObject(ObjectOutput output, EntityKey key) throws IOException {
		output.writeInt( VERSION );
		output.writeUTF( key.getTable() );
		output.writeObject( key.getColumnNames() );
		output.writeObject( key.getColumnValues() );
	}

	@Override
	public EntityKey readObject(ObjectInput input) throws IOException, ClassNotFoundException {
		// version
		input.readInt();

		String tableName = input.readUTF();
		String[] columnNames = (String[]) input.readObject();
		Object[] values = (Object[]) input.readObject();

		return new EntityKey( new EntityKeyMetadata( tableName, columnNames ), values );
	}

	@Override
	public Set<Class<? extends EntityKey>> getTypeClasses() {
		return TYPE_CLASSES;
	}

	@Override
	public Integer getId() {
		return ExternalizerIds.ENTITY_KEY;
	}
}
