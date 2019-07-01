package com.github.jenkins.scm_httpclient;

import com.thoughtworks.xstream.XStream;

public class XSteam {
	public static void main(String[] args) {
		XStream xstream = new XStream();
		
		xstream.alias("person", Person.class);
		xstream.alias("phonenumber", PhoneNumber.class);
		
		Person joe = new Person("Joe", "Walnes");
		joe.setPhone(new PhoneNumber(123, "1234-456"));
		joe.setFax(new PhoneNumber(123, "9999-999"));
		
		// serializing an object to XML
		String xml = xstream.toXML(joe);
		System.out.println(xml);
		
		// deserilizing object back from XML
		Person newJoe = (Person)xstream.fromXML(xml);
		System.out.println(newJoe.getFirstname());
	}

}
