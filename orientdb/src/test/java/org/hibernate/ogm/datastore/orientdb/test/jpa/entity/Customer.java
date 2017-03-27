/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.test.jpa.entity;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.hibernate.annotations.Type;
import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;

import com.orientechnologies.orient.core.id.ORecordId;

/**
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
@Entity
public class Customer implements Serializable {

	private static final long serialVersionUID = 1L;

	private static Log log = LoggerFactory.getLogger();

	@Id
	@Column(name = "bKey")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long bKey;
	@Version
	private int version;
	@Column(name = "@rid")
	private ORecordId rid;

	private String name;
	@OneToMany(mappedBy = "owner")
	private List<BuyingOrder> orders;

	@Enumerated(EnumType.STRING)
	private Status status;

	@Temporal(TemporalType.DATE)
	private Date createdDate;

	@Type(type = "yes_no")
	private boolean blocked;
	@Column(unique = true)
	private String customerNumber;

	@PrePersist
	public void prePersist() {
		setCreatedDate( Calendar.getInstance().getTime() );
		setBlocked( false );
	}

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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<BuyingOrder> getOrders() {
		return orders;
	}

	public void setOrders(List<BuyingOrder> orders) {
		this.orders = orders;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public boolean isBlocked() {
		return blocked;
	}

	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}

	public String getCustomerNumber() {
		return customerNumber;
	}

	public void setCustomerNumber(String customerNumber) {
		this.customerNumber = customerNumber;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 97 * hash + Objects.hashCode( this.bKey );
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
		final Customer other = (Customer) obj;
		if ( !Objects.equals( this.bKey, other.bKey ) ) {
			return false;
		}
		return true;
	}

}
