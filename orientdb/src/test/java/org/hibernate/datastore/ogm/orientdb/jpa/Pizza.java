/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 * 
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.datastore.ogm.orientdb.jpa;

import com.orientechnologies.orient.core.id.ORecordId;
import java.util.List;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Version;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Sergey Chernolyas <sergey.chernolyas@gmail.com>
 */
@Entity
@Indexed(index = "Pizza")
public class Pizza {

	@Id
	@Column(name = "bKey")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long bKey;
	private String name;
	@OneToMany(mappedBy = "buying")
	private List<OrderItem> orderItems;
	@Version
	@Column(name = "@version")
	private int version;
	@Column(name = "@rid")
	private ORecordId rid;

	public Long getbKey() {
		return bKey;
	}

	public void setbKey(Long bKey) {
		this.bKey = bKey;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<OrderItem> getOrderItems() {
		return orderItems;
	}

	public void setOrderItems(List<OrderItem> orderItems) {
		this.orderItems = orderItems;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public ORecordId getRid() {
		return rid;
	}

	public void setRid(ORecordId rid) {
		this.rid = rid;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 13 * hash + Objects.hashCode( this.bKey );
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final Pizza other = (Pizza) obj;
		if ( !Objects.equals( this.bKey, other.bKey ) ) {
			return false;
		}
		return true;
	}

}
