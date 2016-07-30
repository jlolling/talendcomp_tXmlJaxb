package de.cimt.talendcomp.xmldynamic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides the values from an given TXMLObject or List and joins the 
 * possible various arrays to one joined record.
 * 
 * @author jan.lolling@cimt-ag.de
 *
 */
public class ResultValueProvider {

	private List<TXMLObject> resultObjectList = new ArrayList<TXMLObject>();
	private Map<String, List<Object>> fieldListMap = new HashMap<String, List<Object>>();
	private int currentMaxArraySize = 1;
	private int currentIndex = 0;
	
	public void set(List<TXMLObject> resultList) {
		resultObjectList = resultList;
	}
	
	public void addField(String field) {
		List<Object> valueList = new ArrayList<Object>();
		fieldListMap.put(field, valueList);
	}
	
	public boolean next() {
		// first check the value list to iterate through
		
		// if the value lists are done, proceed with the next object
		
		return false;
	}
	

	
}
