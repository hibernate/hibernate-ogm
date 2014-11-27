/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;

import org.hibernate.ogm.datastore.infinispan.InfinispanDialect;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.common.externalizer.impl.ExternalizerIds;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.common.externalizer.impl.VersionChecker;
import org.infinispan.commons.marshall.AdvancedExternalizer;

/**
 * An externalizer for serializing and de-serializing {@link PersistentIdSourceKey} instances. Implicitly used by
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
public class PersistentIdSourceKeyExternalizer implements AdvancedExternalizer<PersistentIdSourceKey> {

	public static final PersistentIdSourceKeyExternalizer INSTANCE = new PersistentIdSourceKeyExternalizer();

	/**
	 * Format version of the key type; allows to apply version dependent deserialization logic in the future if
	 * required; to be incremented when adding new fields to the serialized structure
	 */
	private static final int VERSION = 1;

	private static final Set<Class<? extends PersistentIdSourceKey>> TYPE_CLASSES = Collections
			.<Class<? extends PersistentIdSourceKey>>singleton( PersistentIdSourceKey.class );

	private PersistentIdSourceKeyExternalizer() {
	}

	@Override
	public void writeObject(ObjectOutput output, PersistentIdSourceKey key) throws IOException {
		output.writeInt( VERSION );
		output.writeObject( key.getName() );
		output.writeObject( key.getValue() );
	}

	@Override
	public PersistentIdSourceKey readObject(ObjectInput input) throws IOException, ClassNotFoundException {
		VersionChecker.readAndCheckVersion( input, VERSION, PersistentIdSourceKey.class );

		String name = (String) input.readObject();
		Object value = input.readObject();

		return new PersistentIdSourceKey( name, value );
	}

	@Override
	public Set<Class<? extends PersistentIdSourceKey>> getTypeClasses() {
		return TYPE_CLASSES;
	}

	@Override
	public Integer getId() {
		return ExternalizerIds.PER_TABLE_ID_GENERATOR_KEY;
	}
}
