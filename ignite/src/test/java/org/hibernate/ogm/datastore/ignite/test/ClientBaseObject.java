package org.hibernate.ogm.datastore.ignite.test;

public class ClientBaseObject implements Comparable<ClientBaseObject> {

	protected ObjectId id;
	protected ObjectId clientId;
	protected boolean changed = false;
	
	public ObjectId getId() {
		return id;
	}

	void setId(ObjectId id) {
		this.id = id;
	}

	public ObjectId getClientId() {
		return clientId;
	}

	public void setClientId(ObjectId clientId) {
		this.clientId = clientId;
	}

	public boolean isChanged() {
		return changed;
	}

	public void setChanged(boolean changed) {
		this.changed = changed;
	}

	@Override
	public int compareTo(ClientBaseObject o) {
		if (id == null || clientId == null) throw new NullPointerException("ID is null");
		if (o != null){
			if (clientId.compareTo(o.getClientId()) == 0)
				return (o != this) ? ((o != null) ? id.compareTo(o.getId()) : 1) : 0;
			else
				return clientId.compareTo(o.getClientId());
		}
		else 
			return 1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((clientId == null) ? 0 : clientId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		ClientBaseObject other = (ClientBaseObject) obj;
		if (clientId == null) {
			if (other.clientId != null)
				return false;
		} else if (!clientId.equals(other.clientId))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
}
