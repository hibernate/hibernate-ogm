/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.utils.test.associations;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.ogm.datastore.ignite.options.CacheStoreFactory;
import org.hibernate.ogm.datastore.ignite.options.ReadThrough;
import org.hibernate.ogm.datastore.ignite.options.StoreKeepBinary;
import org.hibernate.ogm.datastore.ignite.options.WriteThrough;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
@Entity
@ReadThrough
@WriteThrough
@StoreKeepBinary
@CacheStoreFactory(JUGBinaryStore.class)
public class CacheStoreJUG implements Serializable {
	private String id;
	private String name;
	private List<CacheStoreMember> members = new LinkedList<>();

	public CacheStoreJUG() {
	}

	public CacheStoreJUG(String id) {
		this.id = id;
	}

	@Id
	@Column(name = "jug_id")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	@OneToMany(mappedBy = "memberOf")
	public List<CacheStoreMember> getMembers() {
		return members;
	}

	public void setMembers(List<CacheStoreMember> members) {
		this.members = members;
	}
}
