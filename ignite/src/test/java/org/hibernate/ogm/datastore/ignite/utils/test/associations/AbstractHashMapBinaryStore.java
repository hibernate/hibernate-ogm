/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.utils.test.associations;

import java.util.Map;
import javax.cache.Cache;
import javax.cache.integration.CacheWriterException;

import org.hibernate.ogm.datastore.ignite.logging.impl.Log;
import org.hibernate.ogm.datastore.ignite.logging.impl.LoggerFactory;

import org.apache.ignite.Ignite;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryType;
import org.apache.ignite.cache.store.CacheStoreAdapter;
import org.apache.ignite.resources.IgniteInstanceResource;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public abstract class AbstractHashMapBinaryStore extends CacheStoreAdapter<String, BinaryObject> {
	private Log logger = LoggerFactory.getLogger();

	@IgniteInstanceResource
	private Ignite ignite;

	protected abstract Map<String, BinaryObject> getStore();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BinaryObject load(String key) {
		logger.debugf( "Load object by ID %s", key );
		return getStore().get( key );
	}

	@Override
	public void write(Cache.Entry<? extends String, ? extends BinaryObject> entry) throws CacheWriterException {
		logger.infof( "try to write object with ID %s", entry.getKey() );
		BinaryObject obj = entry.getValue();

		BinaryType type = obj.type();

		logger.infof( "fields of type %s is %s", type.typeName(), type.fieldNames() );

		getStore().put( entry.getKey(), obj );
	}

	@Override
	public void delete(Object key) throws CacheWriterException {
		logger.infof( "try to delete object with ID %s (class: %s)", key, key.getClass() );
		getStore().remove( key );
	}
}
