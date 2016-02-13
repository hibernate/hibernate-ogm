/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PostPersist;
import javax.persistence.Version;
import org.hibernate.datastore.ogm.orientdb.logging.impl.Log;
import org.hibernate.datastore.ogm.orientdb.logging.impl.LoggerFactory;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author chernolyassv
 */
@Entity
@Indexed(index = "Customer")
@NamedQueries({
		@NamedQuery(name = "Customer.findAll", query = "SELECT c FROM Customer c"),
		@NamedQuery(name = "Country.findByName", query = "SELECT c FROM Customer c WHERE c.name = :name") })
public class Customer {
    private static Log log = LoggerFactory.getLogger();

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
	private String name;
	@OneToMany(mappedBy = "owner")
	private List<BuyingOrder> orders;
        
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
