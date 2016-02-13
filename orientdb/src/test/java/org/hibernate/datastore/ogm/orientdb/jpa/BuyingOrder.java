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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Version;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.orientdb.bridge.ORecordIdTwoWayStringBridge;

/**
 * @author Sergey Chernolyas <sergey.chernolyas@gmail.com>
 */

@Entity
@Indexed(index = "BuyingOrder")
public class BuyingOrder {

	@Id
	@Column(name = "bKey")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long bKey;
	@Version
	@Column(name = "@version")
	private int version;
	@Column(name = "@rid")
	private ORecordId rid;
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.YES)
	private String orderKey;
	@ManyToOne
	private Customer owner;
	@OneToMany(mappedBy = "order")
	private List<OrderItem> orders;

	public Long getbKey() {
		return bKey;
	}

	public void setbKey(Long bKey) {
		this.bKey = bKey;
	}

	public ORecordId getRid() {
		return rid;
	}

	public void setRid(ORecordId rid) {
		this.rid = rid;
	}

	public String getOrderKey() {
		return orderKey;
	}

	public void setOrderKey(String orderKey) {
		this.orderKey = orderKey;
	}

	public Customer getOwner() {
		return owner;
	}

	public void setOwner(Customer owner) {
		this.owner = owner;
	}

	public List<OrderItem> getOrders() {
		return orders;
	}

	public void setOrders(List<OrderItem> orders) {
		this.orders = orders;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 11 * hash + Objects.hashCode( this.bKey );
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
		final BuyingOrder other = (BuyingOrder) obj;
		if ( !Objects.equals( this.bKey, other.bKey ) ) {
			return false;
		}
		return true;
	}

}
