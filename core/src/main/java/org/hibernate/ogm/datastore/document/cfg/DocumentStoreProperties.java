/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.cfg;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.document.options.MapStorageType;

/**
 * Common properties for configuring document datastores such as MongoDB or CouchDB via {@code persistence.xml} or
 * {@link StandardServiceRegistryBuilder#applySetting(String, Object)}.
 * <p>
 * Note that not all properties are supported by all datastores; refer to the documentation of the specific dialect to
 * find out the supported configuration properties.
 * <p>
 * This interface should not be implemented by client code, only its constants are intended to be referenced.
 *
 * @author Gunnar Morling
 */
public interface DocumentStoreProperties extends OgmProperties {

	/**
	 * Property for configuring the strategy for storing associations. Valid values are the
	 * {@link org.hibernate.ogm.datastore.document.options.AssociationStorageType} enumeration and the String
	 * representation of its constants. Defaults to the in-entity storage strategy.
	 * <p>
	 * Note that any value specified via this property will be overridden by values configured via annotations or the
	 * programmatic API.
	 */
	String ASSOCIATIONS_STORE = "hibernate.ogm.datastore.document.association_storage";

	/**
	 * Property for configuring the strategy for storing map-typed associations. Only applies to maps with a single key
	 * column which is of type {@code String}. Valid values are the {@link MapStorageType} enumeration and the String
	 * representation of its constants. Defaults to the {@link MapStorageType#BY_KEY} strategy. For map associations
	 * with more than one key column or a single key column of another type than {@code String} always the list strategy
	 * will be used regardless of this setting.
	 * <p>
	 * Note that any value specified via this property will be overridden by values configured via annotations or the
	 * programmatic API.
	 */
	String MAP_STORAGE = "hibernate.ogm.datastore.document.map_storage";
}
