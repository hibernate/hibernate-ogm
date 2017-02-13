/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.test.jpa.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.annotations.Type;

/**
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
@Entity
public class SimpleTypesEntity implements Serializable {

	public static enum EnumType {
		E1, E2;
	}

	@Id
	private Long id;

	private Integer intValue;
	private Short shortValue;
	private Byte byteValue;

	private Boolean booleanValue1;
	@Type(type = "true_false")
	private boolean tfBooleanValue;

	@Type(type = "yes_no")
	private boolean yesNoBooleanValue;

	@Type(type = "numeric_boolean")
	private boolean numericBooleanValue;

	@Enumerated(javax.persistence.EnumType.ORDINAL)
	private EnumType e1;

	@Enumerated(javax.persistence.EnumType.STRING)
	private EnumType e2;

	@Temporal(TemporalType.TIMESTAMP)
	private Date createdTimestamp;

	@Temporal(TemporalType.DATE)
	private Date createdDate;

	public SimpleTypesEntity() {
	}

	public SimpleTypesEntity(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getIntValue() {
		return intValue;
	}

	public void setIntValue(Integer intValue) {
		this.intValue = intValue;
	}

	public Short getShortValue() {
		return shortValue;
	}

	public void setShortValue(Short shortValue) {
		this.shortValue = shortValue;
	}

	public Byte getByteValue() {
		return byteValue;
	}

	public void setByteValue(Byte byteValue) {
		this.byteValue = byteValue;
	}

	public Boolean getBooleanValue1() {
		return booleanValue1;
	}

	public void setBooleanValue1(Boolean booleanValue1) {
		this.booleanValue1 = booleanValue1;
	}

	public boolean isTfBooleanValue() {
		return tfBooleanValue;
	}

	public void setTfBooleanValue(boolean tfBooleanValue) {
		this.tfBooleanValue = tfBooleanValue;
	}

	public boolean isYesNoBooleanValue() {
		return yesNoBooleanValue;
	}

	public void setYesNoBooleanValue(boolean yesNoBooleanValue) {
		this.yesNoBooleanValue = yesNoBooleanValue;
	}

	public boolean isNumericBooleanValue() {
		return numericBooleanValue;
	}

	public void setNumericBooleanValue(boolean numericBooleanValue) {
		this.numericBooleanValue = numericBooleanValue;
	}

	public EnumType getE1() {
		return e1;
	}

	public void setE1(EnumType e1) {
		this.e1 = e1;
	}

	public EnumType getE2() {
		return e2;
	}

	public void setE2(EnumType e2) {
		this.e2 = e2;
	}

	public Date getCreatedTimestamp() {
		return createdTimestamp;
	}

	public void setCreatedTimestamp(Date createdTimestamp) {
		this.createdTimestamp = createdTimestamp;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 47 * hash + Objects.hashCode( this.id );
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
		final SimpleTypesEntity other = (SimpleTypesEntity) obj;
		if ( !Objects.equals( this.id, other.id ) ) {
			return false;
		}
		return true;
	}

}
