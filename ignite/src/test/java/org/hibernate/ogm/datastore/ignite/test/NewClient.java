package org.hibernate.ogm.datastore.ignite.test;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Persister;

@Table(schema="CLIENT_PERSON", name="Client")
@Entity
@Persister(impl=org.hibernate.ogm.datastore.ignite.persister.impl.IgniteSingleTableEntityPersister.class)
public class NewClient {
	
	@EmbeddedId
	private ObjectId id;
	@Basic
	@Column(name="NAME_")
	private String name;
	@Basic
	@Column(name="INFO")
	private String information;
	@Basic
	@Column(name="FIELD")
	private String field;
	
	public NewClient() {
		
	}
	
	public NewClient(ObjectId id, String name, String information, String field){
		this.id = id;
		this.name = name;
		this.information = information;
		this.field = field;
	}

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getInformation() {
		return information;
	}

	public void setInformation(String information) {
		this.information = information;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((information == null) ? 0 : information.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NewClient other = (NewClient) obj;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (information == null) {
			if (other.information != null)
				return false;
		} else if (!information.equals(other.information))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
