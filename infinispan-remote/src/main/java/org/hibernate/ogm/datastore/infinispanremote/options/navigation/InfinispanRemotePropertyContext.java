/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.options.navigation;

import org.hibernate.ogm.datastore.keyvalue.options.navigation.KeyValueStorePropertyContext;

/**
 * Allows to configure Infinispan Remote specific options for a single property.
 *
 * @author Gunnar Morling
 */
public interface InfinispanRemotePropertyContext extends KeyValueStorePropertyContext<InfinispanRemoteEntityContext, InfinispanRemotePropertyContext> {
}
