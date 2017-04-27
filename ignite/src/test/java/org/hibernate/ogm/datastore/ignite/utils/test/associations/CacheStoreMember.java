/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.utils.test.associations;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

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
@CacheStoreFactory(MemberBinaryStore.class)
public class CacheStoreMember implements Serializable {
	private String id;
	private String name;
	private CacheStoreJUG memberOf;

	public CacheStoreMember() {
	}

	public CacheStoreMember(String id) {
		this.id = id;
	}

	@Id
	@Column(name = "member_id")
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

	@ManyToOne
	public CacheStoreJUG getMemberOf() {
		return memberOf;
	}

	public void setMemberOf(CacheStoreJUG memberOf) {
		this.memberOf = memberOf;
	}
}
