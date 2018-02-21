/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.template;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.ogm.datastore.infinispanremote.options.cache.CacheTemplate;

/**
 * Used a NOT existing cache as a template configuration
 *
 * @author Fabio Massimo Ercoli
 */
@Entity
@CacheTemplate("notExist")
public class NotExistCacheTemplateEntity {

	@Id
	private String id;

}
