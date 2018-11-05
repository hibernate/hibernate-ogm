/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.order;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

@Entity
public class Meeting {

	@Id
	private String name;

	@ElementCollection
	private List<String> addresses = new ArrayList<>();

	@ElementCollection
	@OrderColumn(name = "relevance")
	private List<String> phones = new ArrayList<>();

	@OneToMany
	@JoinTable(name = "Meeting_speakers")
	@OrderColumn(name = "presentationOrder")
	private List<Programmer> speakers = new ArrayList<>();

	@OneToMany
	private List<Programmer> participants = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getAddresses() {
		return addresses;
	}

	public List<String> getPhones() {
		return phones;
	}

	public List<Programmer> getSpeakers() {
		return speakers;
	}

	public List<Programmer> getParticipants() {
		return participants;
	}

	public void addAddress(String address) {
		this.addresses.add( address );
	}

	public void addPhone(String phone) {
		this.phones.add( phone );
	}

	public void addSpeaker(Programmer speaker) {
		this.speakers.add( speaker );
	}

	public void addParticipant(Programmer participant) {
		this.participants.add( participant );
	}
}
