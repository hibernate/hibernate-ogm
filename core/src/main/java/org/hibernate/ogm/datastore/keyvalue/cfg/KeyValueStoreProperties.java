/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.keyvalue.cfg;

import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.cfg.OgmProperties;

/**
 * Common properties for configuring key/value datastores such as Infinispan or Ehcache via {@code persistence.xml} or
 * {@link OgmConfiguration}.
 * <p>
 * Note that not all properties are supported by all datastores; refer to the documentation of the specific dialect to
 * find out the supported configuration properties.
 * <p>
 * This interface should not be implemented by client code, only its constants are intended to be referenced.
 *
 * @author Gunnar Morling
 */
public interface KeyValueStoreProperties extends OgmProperties {

	/**
	 * The configuration property for setting the cache mapping. Supported values are the
	 * {@link org.hibernate.ogm.datastore.keyvalue.options.CacheMappingType} enum or
	 * the String representations of its constants. Defaults to {@link org.hibernate.ogm.datastore.keyvalue.options.CacheMappingType#CACHE_PER_TABLE}.
	 * <p>
	 * Note that any value specified via this property will be overridden by values configured via annotations or the
	 * programmatic API.
	 */
	String CACHE_MAPPING = "hibernate.ogm.datastore.keyvalue.cache_mapping";
}
